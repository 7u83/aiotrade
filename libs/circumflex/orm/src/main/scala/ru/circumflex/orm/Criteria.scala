package ru.circumflex.orm

import collection.mutable.ListBuffer
import ORM._
import ru.circumflex.orm.DMLQuery.Delete
import ru.circumflex.orm.Predicate.EmptyPredicate
import ru.circumflex.orm.Projection.RecordProjection
import ru.circumflex.orm.Query.Order
import ru.circumflex.orm.Query.SQLQuery
import ru.circumflex.orm.RelationNode.ChildToParentJoin
import ru.circumflex.orm.RelationNode.JoinNode
import ru.circumflex.orm.RelationNode.LeftJoin
import ru.circumflex.orm.RelationNode.ParentToChildJoin

/**
 * Criteria is high-level abstraction of <code>Select</code>. It is used to operate
 * specifically with records and simplifies record querying.
 * Note that if you want to use projections, you should still use lower-level <code>Select</code>
 */
class Criteria[R: Manifest](val rootNode: RelationNode[R]) extends SQLable with Cloneable {

  private var _counter = 0;

  protected var _rootTree: RelationNode[R] = rootNode
  protected var _joinTree: RelationNode[R] = rootNode
  protected var _prefetchSeq: Seq[Association[_, _]] = Nil

  protected var _projections: Seq[RecordProjection[_]] = List(rootNode.*)
  protected var _restrictions: Seq[Predicate] = Nil
  protected var _orders: Seq[Order] = Nil
  protected var _limit = -1;
  protected var _offset = 0;

  def toSql = prepareQuery.toSql

  def prepareQuery: SQLQuery = select(_projections: _*)
          .from(prepareQueryPlan)
          .where(preparePredicate)
          .orderBy(_orders: _*)

  def preparePredicate: Predicate =
    if (_restrictions.size > 0)
      and(_restrictions: _*)
    else EmptyPredicate

  /**
   * Merges join tree with prefetch tree to form the actual FROM clause.
   */
  def prepareQueryPlan: RelationNode[R] = _joinTree match {
    case j: JoinNode[R, _] =>
      replaceLeft(j.clone, _rootTree)
    case r: RelationNode[R] =>    // no changes to the root tree required
      return _rootTree
  }

  /**
   * Replaces the left-most node of specified join node with specified node.
   */
  protected def replaceLeft[R](join: JoinNode[R, _],
                               node: RelationNode[R]): RelationNode[R] =
    join.left match {
      case j: JoinNode[R, _] => replaceLeft(j, node)
      case r: RelationNode[R] => join.replaceLeft(node)
    }

  /* COMMON STUFF */

  /**
   * Sets the maximum results for this query.
   */
  def limit(value: Int): this.type = {
    _limit = value
    return this
  }

  /**
   * Sets the result offset for this query.
   */
  def offset(value: Int): this.type = {
    _offset = value
    return this
  }

  /**
   * Adds arbitrary predicate to this query's restrictions list.
   * Restrictions are assembled into conjunction prior to query execution.
   */
  def add(predicate: Predicate): this.type = {
    _restrictions ++= List(predicate)
    return this
  }

  /**
   * Adds a predicate represented by provided expression and parametes.
   */
  def add(expression: String, params: Pair[String,Any]*): this.type =
    add(prepareExpr(expression, params: _*))

  /**
   * Adds a predicate related to criteria root node to this query's restrictions list.
   * Restrictions are assembled into conjunction prior to query execution.
   */
  def add(nodeToPredicate: RelationNode[R] => Predicate): this.type =
    add(nodeToPredicate(rootNode))

  /**
   * Adds an order to ORDER BY clause.
   */
  def orderBy(orders: Order*): this.type = {
    _orders ++= orders.toList
    return this
  }

  /* JOINS STUFF */

  /**
   * Adds, if possible, a left join with specified nodes.
   * The projections of joined nodes are not fetched.
   * This method is only useful when search criteria are applied to
   * other relation, then <code>rootNode</code>.
   */
  def join(nodes: RelationNode[_]*): this.type = {
    nodes.toList.foreach(n =>
      _joinTree = updateJoinTree(n, _joinTree))
    return this
  }

  protected def updateJoinTree[R](node: RelationNode[_],
                                  tree: RelationNode[R]): RelationNode[R] =
    tree match {
      case j: JoinNode[R, _] => try {     // try to update left node
        j.replaceLeft(updateJoinTree(node, j.left))
      } catch {
        case e: ORMException =>   // join with left side failed; try the right side
          j.replaceRight(updateJoinTree(node, j.right))
      }
      case rel: RelationNode[R] => rel.join(node)
    }

  /* PREFETCH STUFF */

  /**
   * Adds a join to query tree for prefetching.
   */
  def prefetch(association: Association[Any, Any]*): this.type = {
    association.toList.foreach(a => if (!_prefetchSeq.contains(a)) {
      // do the depth-search and update query plan tree
      _rootTree = updateRootTree[R](a, _rootTree)
      // also add the prefetch list of each relation
      prefetch(a.parentRelation.prefetchList: _*)
      prefetch(a.childRelation.prefetchList: _*)
    })
    return this
  }

  protected def updateRootTree[R: Manifest](association: Association[_, _],
                                  node: RelationNode[R]): RelationNode[R] =
    node match {
      case j: JoinNode[R, Any] => j
              .replaceLeft(updateRootTree(association, j.left))
              .replaceRight(updateRootTree(association, j.right))
      case rel: RelationNode[_] =>
        if (rel.equals(association.parentRelation)) {
          val a = association.asInstanceOf[Association[Any, R]]
          new ParentToChildJoin[R, Any](rel, makePrefetch(a, a.childRelation), a, LeftJoin)
        }
        else if (rel.equals(association.childRelation)) {
          val a = association.asInstanceOf[Association[R, Any]]
          new ChildToParentJoin(rel, makePrefetch(a, a.parentRelation), a, LeftJoin)
        }
        else rel
    }

  protected def makePrefetch[R](association: Association[_, _],
                                rel: Relation[R]): RelationNode[R] = {
    _counter += 1
    val node = rel.as("pf_" + _counter)
    _projections ++= List(node.*)
    _prefetchSeq ++= List(association)
    return node
  }

  /* QUERING EXECUTORS */

  /**
   * Executes the query and retrieves records list.
   */
  def list: Seq[R] = {
    val q = prepareQuery
    q.resultSet(rs => {
      val result = new ListBuffer[R]()
      while (rs.next) {
        val tuple = q.readTuple(rs)
        processTupleTree(tuple, _rootTree)
        val root = tuple.apply(0).asInstanceOf[R]
        if (!result.contains(root)) result += root
      }
      return result
    })
  }

  protected def processTupleTree(tuple: Array[_], tree: RelationNode[_]): Unit = tree match {
    case j: JoinNode[_, _] =>
      val pNode = j match {
        case j: ParentToChildJoin[_, _] => j.left
        case j: ChildToParentJoin[_, _] => j.right
        case _ => return
      }
      val cNode = j match {
        case j: ParentToChildJoin[_, _] => j.right
        case j: ChildToParentJoin[_, _] => j.left
        case _ => return
      }
      val a = j match {
        case j: ParentToChildJoin[_, _] => j.association
        case j: ChildToParentJoin[_, _] => j.association
        case _ => return
      }
      val pIndex = _projections.findIndexOf(p => p.node.alias == pNode.alias)
      val cIndex = _projections.findIndexOf(p => p.node.alias == cNode.alias)
      if (pIndex == -1 || cIndex == -1) return
      val parent = tuple(pIndex)
      val child = tuple(cIndex)
      cacheAssociation(a.asInstanceOf[Association[Any, Any]], child, parent)
      processTupleTree(tuple, j.left)
      processTupleTree(tuple, j.right)
    case _ =>
  }

  protected def cacheAssociation[C, P](a: Association[C, P],
                                       child: C,
                                       parent: P): Unit = {
    if (child != null)
      tx.updateMTOCache(a, child, parent)
    if (parent != null) {
      var otm = tx.getCachedOTM(a, parent) match {
        case Some(s: Seq[C]) => s
        case _ => Nil
      }
      if (child != null && !otm.contains(child))
        otm ++= List(child)
      tx.updateOTMCache(a, parent, otm)
    }
  }

  /**
   * Executes the query and retrieves a unique root record. If result set yields multiple
   * root records an exception is thrown.
   */
  def unique: Option[R] = {
    val q = prepareQuery
    q.resultSet(rs => {
      if (!rs.next) return None     // none found
      // found, extracting first root as a result
      val firstTuple = q.readTuple(rs)
      processTupleTree(firstTuple, _rootTree)
      val result = firstTuple.apply(0).asInstanceOf[R]
      // process any other tuples, but make sure there's no other roots among them
      while (rs.next) {
        val tuple = q.readTuple(rs)
        processTupleTree(tuple, _rootTree)
        val root = tuple.apply(0).asInstanceOf[R]
        if (root != result)   // wow, this thingy should not be here, call the police
          throw new ORMException("Unique result expected, but multiple records found.")
      }
      return Some(result)
    })
  }


  /**
   * Executes the DELETE statement for this relation using specified criteria.
   */
  def delete(): Int = new Delete(rootNode).where(preparePredicate).executeUpdate

}