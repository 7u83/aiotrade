/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.chartview

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.ResourceBundle
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents

import org.aiotrade.lib.securities.Sec
import org.aiotrade.lib.securities.Ticker
import org.aiotrade.lib.securities.TickerObserver
import org.aiotrade.lib.securities.TickerSnapshot
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.util.swing.GBC
import org.aiotrade.lib.util.swing.plaf.AIOScrollPaneStyleBorder
import org.aiotrade.lib.util.swing.table.AttributiveCellRenderer
import org.aiotrade.lib.util.swing.table.AttributiveCellTableModel
import org.aiotrade.lib.util.swing.table.DefaultCellAttribute
import org.aiotrade.lib.util.swing.table.MultiSpanCellTable

/**
 *
 * @author Caoyuan Deng
 */
object RealTimeBoardPanel {
  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.chartview.chartview.Bundle")
  private val NUMBER_FORMAT = NumberFormat.getInstance
}

class RealTimeBoardPanel(sec: Sec, contents: AnalysisContents) extends JPanel with TickerObserver[TickerSnapshot] {
  import RealTimeBoardPanel._

  private var tickerContract: TickerContract = _
  private var prevTicker: Ticker = _
  private var infoModel: DefaultTableModel = _
  private var depthModel: DefaultTableModel = _
  private var tickerModel: DefaultTableModel = _
  private var infoCellAttr: DefaultCellAttribute = _
  private var depthCellAttr: DefaultCellAttribute = _
  private var marketCal: Calendar = _
  private var viewContainer: RealTimeChartViewContainer = _
  private var infoTable: JTable = _
  private var depthTable: JTable = _
  private var tickerTable: JTable = _
  private var tickerPane: JScrollPane = _
  private val sdf: SimpleDateFormat = new SimpleDateFormat("HH:mm:ss")
  private val symbol = new ValueCell
  private val sname = new ValueCell
  private val currentTime = new ValueCell
  private val dayChange = new ValueCell
  private val dayHigh = new ValueCell
  private val dayLow = new ValueCell
  private val dayOpen = new ValueCell
  private val dayVolume = new ValueCell
  private val lastPrice = new ValueCell
  private val dayPercent = new ValueCell
  private val prevClose = new ValueCell

  /**
   * Creates new form RealtimeBoardPanel
   */

  val timeZone = sec.market.timeZone
  this.marketCal = Calendar.getInstance(timeZone)
  this.sdf.setTimeZone(timeZone)
  this.tickerContract = sec.tickerContract
  initComponents

  private var columeModel = infoTable.getColumnModel
  columeModel.getColumn(0).setMaxWidth(35)
  columeModel.getColumn(2).setMaxWidth(35)

  columeModel = depthTable.getColumnModel
  columeModel.getColumn(0).setMinWidth(12)
  columeModel.getColumn(1).setMinWidth(35)

  columeModel = tickerTable.getColumnModel
  columeModel.getColumn(0).setMinWidth(22)
  columeModel.getColumn(1).setMinWidth(30)

//        ChartingController controller = ChartingControllerFactory.createInstance(
//                sec.getTickerSer(), contents);
//        viewContainer = controller.createChartViewContainer(
//                RealTimeChartViewContainer.class, this);
//
//        chartPane.setLayout(new BorderLayout());
//        chartPane.add(viewContainer, BorderLayout.CENTER);


  private def initComponents {
    setFocusable(false)

    tickerPane = new JScrollPane
    //chartPane = new JPanel();

    val infoModelData = Array(
      Array(BUNDLE.getString("lastPrice"), lastPrice, BUNDLE.getString("dayVolume"), dayVolume),
      Array(BUNDLE.getString("dayChange"), dayChange, BUNDLE.getString("dayHigh"), dayHigh),
      Array(BUNDLE.getString("dayPercent"), dayPercent, BUNDLE.getString("dayLow"), dayLow),
      Array(BUNDLE.getString("prevClose"), prevClose, BUNDLE.getString("dayOpen"), dayOpen)
    )
    infoModel = new AttributiveCellTableModel(
      infoModelData.asInstanceOf[Array[Array[Any]]],
      Array("A", "B", "C", "D").asInstanceOf[Array[Any]]
    )

    infoCellAttr = infoModel.asInstanceOf[AttributiveCellTableModel].getCellAttribute.asInstanceOf[DefaultCellAttribute]
    /* Code for combining cells
     infoCellAttr.combine(new int[]{0}, new int[]{0, 1});
     infoCellAttr.combine(new int[]{1}, new int[]{0, 1, 2, 3});
     */

    ValueCell.setRowColumn(infoModelData)
    symbol.value = sec.uniSymbol
    if (tickerContract != null) {
      sname.value = tickerContract.shortName
    }

    for (cell <- Array(
        lastPrice, dayChange, dayPercent, prevClose, dayVolume, dayHigh, dayLow, dayOpen
      )) {
      infoCellAttr.setHorizontalAlignment(SwingConstants.TRAILING, cell.row, cell.column)
    }

    infoTable = new MultiSpanCellTable(infoModel)
    infoTable.setDefaultRenderer(classOf[Object], new AttributiveCellRenderer)
    infoTable.setFocusable(false);
    infoTable.setCellSelectionEnabled(false);
    infoTable.setShowHorizontalLines(false);
    infoTable.setShowVerticalLines(false);
    infoTable.setBorder(new AIOScrollPaneStyleBorder(LookFeel.getCurrent.heavyBackgroundColor))
    infoTable.setBackground(LookFeel.getCurrent.heavyBackgroundColor)

    depthModel = new AttributiveCellTableModel(
      Array(
        Array("卖⑤", null, null),
        Array("卖④", null, null),
        Array("卖③", null, null),
        Array("卖②", null, null),
        Array("卖①", null, null),
        Array("成交", null, null),
        Array("买①", null, null),
        Array("买②", null, null),
        Array("买③", null, null),
        Array("买④", null, null),
        Array("买⑤", null, null)
      ).asInstanceOf[Array[Array[Any]]],
      Array(
        BUNDLE.getString("askBid"), BUNDLE.getString("price"), BUNDLE.getString("size")
      ).asInstanceOf[Array[Any]]
    )

    val depth = 5
    val dealRow = 5
    depthModel.setValueAt(BUNDLE.getString("deal"), dealRow, 0)
    for (i <- 0 until depth) {
      val askIdx = depth - 1 - i
      val askRow = i
      depthModel.setValueAt(BUNDLE.getString("bid") + numbers(askIdx), askRow, 0)
      val bidIdx = i
      val bidRow = depth + 1 + i
      depthModel.setValueAt(BUNDLE.getString("ask") + numbers(bidIdx), bidRow, 0)
    }

    depthTable = new MultiSpanCellTable(depthModel)
    depthCellAttr = depthModel.asInstanceOf[AttributiveCellTableModel].getCellAttribute.asInstanceOf[DefaultCellAttribute]

    for (i <- 0 until 11) {
      for (j <- 1 until 3) {
        depthCellAttr.setHorizontalAlignment(SwingConstants.TRAILING, i, j)
      }
    }
    depthCellAttr.setHorizontalAlignment(SwingConstants.LEADING, 5, 0)
//        for (int j = 0; j < 3; j++) {
//            depthCellAttr.setBackground(Color.gray, 5, j);
//        }

    depthTable.setDefaultRenderer(classOf[Object], new AttributiveCellRenderer)
    depthTable.setTableHeader(null)
    depthTable.setFocusable(false)
    depthTable.setCellSelectionEnabled(false)
    depthTable.setShowHorizontalLines(false)
    depthTable.setShowVerticalLines(false)
    depthTable.setBorder(new AIOScrollPaneStyleBorder(LookFeel.getCurrent.borderColor))
    depthTable.setBackground(LookFeel.getCurrent.infoBackgroundColor)

    tickerModel = new DefaultTableModel(
      Array(
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null),
        Array(null, null, null)
      ).asInstanceOf[Array[Array[Object]]],
      Array(
        BUNDLE.getString("time"), BUNDLE.getString("price"), BUNDLE.getString("size")
      ).asInstanceOf[Array[Object]]
    ) {

      val canEdit = Array(
        false, false, false
      )

      override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = {
        canEdit(columnIndex)
      }
    }

    tickerTable = new JTable(tickerModel)
    tickerTable.setDefaultRenderer(classOf[Object], new TrendSensitiveCellRenderer)
    tickerTable.setFocusable(false)
    tickerTable.setCellSelectionEnabled(false)
    tickerTable.setShowHorizontalLines(false)
    tickerTable.setShowVerticalLines(false)

    /* @Note Border of JScrollPane may cannot be set by #setBorder, at least in Metal L&F: */
    UIManager.put("ScrollPane.border", classOf[AIOScrollPaneStyleBorder].getName)
    tickerPane.setBackground(LookFeel.getCurrent.infoBackgroundColor)
    tickerPane.setViewportView(tickerTable)

    // put infoTable to a box to simple the insets setting:
    val infoBox = new Box(BoxLayout.Y_AXIS) {

      // box does not paint anything, override paintComponent to get background:
      override protected def paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.setColor(getBackground)
        val rect = getBounds()
        g.fillRect(rect.x, rect.y, rect.width, rect.height)
      }
    };
    infoBox.setBackground(LookFeel.getCurrent.heavyBackgroundColor)
    infoBox.add(Box.createVerticalStrut(5))
    infoBox.add(infoTable)
    infoBox.add(Box.createVerticalStrut(4))

    // put fix size components to box
    val box = Box.createVerticalBox
    box.add(infoBox)
    box.add(Box.createVerticalStrut(2))
    box.add(depthTable)
    box.add(Box.createVerticalStrut(2))

    setLayout(new GridBagLayout)
    add(box, new GBC(0, 0).setFill(GridBagConstraints.BOTH).setWeight(100, 0))
    add(tickerPane, new GBC(0, 1).setFill(GridBagConstraints.BOTH).setWeight(100, 100))
    //add(chartPane, new GBC(0,2).setFill(GBC.BOTH).setWeight(100, 100));
  }
  val numbers = Array("①", "②", "③", "④", "⑤")

  def update(tickerSnapshot: TickerSnapshot) {
    val neutralColor = LookFeel.getCurrent.getNeutralColor
    val positiveColor = LookFeel.getCurrent.getPositiveColor
    val negativeColor = LookFeel.getCurrent.getNegativeColor
    symbol.value = tickerSnapshot.symbol

    val snapshotTicker = tickerSnapshot.ticker

    val currentSize =
      if (prevTicker != null) {
        (snapshotTicker(Ticker.DAY_VOLUME) - prevTicker(Ticker.DAY_VOLUME)).intValue
      } else 0

    val depth = snapshotTicker.depth
    val dealRow = 5
    depthModel.setValueAt(String.format("%8.2f", Array(snapshotTicker(Ticker.LAST_PRICE))), dealRow, 1)
    depthModel.setValueAt(if (prevTicker == null) "-" else currentSize, dealRow, 2)
    for (i <- 0 until depth) {
      val askIdx = depth - 1 - i
      val askRow = i
      depthModel.setValueAt(String.format("%8.2f", Array(snapshotTicker.askPrice(askIdx))), askRow, 1)
      depthModel.setValueAt(String.valueOf(snapshotTicker.askSize(askIdx).intValue), askRow, 2)
      val bidIdx = i
      val bidRow = depth + 1 + i
      depthModel.setValueAt(String.format("%8.2f", Array(snapshotTicker.bidPrice(bidIdx))), bidRow, 1)
      depthModel.setValueAt(String.valueOf(snapshotTicker.bidSize(bidIdx).intValue), bidRow, 2)
    }

    marketCal.setTimeInMillis(snapshotTicker.time)
    val lastTradeTime = marketCal.getTime
    currentTime.value = sdf.format(lastTradeTime)
    lastPrice.value   = String.format("%8.2f",  Array(snapshotTicker(Ticker.LAST_PRICE)))
    prevClose.value   = String.format("%8.2f",  Array(snapshotTicker(Ticker.PREV_CLOSE)))
    dayOpen.value     = String.format("%8.2f",  Array(snapshotTicker(Ticker.DAY_OPEN)))
    dayHigh.value     = String.format("%8.2f",  Array(snapshotTicker(Ticker.DAY_HIGH)))
    dayLow.value      = String.format("%8.2f",  Array(snapshotTicker(Ticker.DAY_LOW)))
    dayChange.value   = String.format("%+8.2f", Array(snapshotTicker(Ticker.DAY_CHANGE)))
    dayPercent.value  = String.format("%+3.2f", Array(snapshotTicker.changeInPercent)) + "%"
    dayVolume.value   = String.valueOf(snapshotTicker(Ticker.DAY_VOLUME))

    var fgColor = Color.BLACK
    var bgColor = neutralColor
    if (snapshotTicker(Ticker.DAY_CHANGE) > 0) {
      fgColor = Color.WHITE
      bgColor = positiveColor
    } else if (snapshotTicker(Ticker.DAY_CHANGE) < 0) {
      fgColor = Color.WHITE
      bgColor = negativeColor
    }
    infoCellAttr.setForeground(fgColor, dayChange.row, dayChange.column)
    infoCellAttr.setForeground(fgColor, dayPercent.row, dayPercent.column)
    infoCellAttr.setBackground(bgColor, dayChange.row, dayChange.column)
    infoCellAttr.setBackground(bgColor, dayPercent.row, dayPercent.column)

    /**
     * Sometimes, DataUpdatedEvent is fired by other symbols' new ticker,
     * so assert here again.
     * @see UpdateServer.class in AbstractTickerDataServer.class and YahooTickerDataServer.class
     */
    if (prevTicker != null && snapshotTicker.isDayVolumeChanged(prevTicker)) {
      fgColor = Color.BLACK;
      bgColor = neutralColor;
      snapshotTicker.compareLastCloseTo(prevTicker) match {
        case 1 =>
          fgColor = Color.WHITE
          bgColor = positiveColor
        case -1 =>
          fgColor = Color.WHITE
          bgColor = negativeColor
        case _ =>
      }

    }
    infoCellAttr.setForeground(fgColor, lastPrice.row, lastPrice.column)
    infoCellAttr.setBackground(bgColor, lastPrice.row, lastPrice.column)
    depthCellAttr.setForeground(fgColor, dealRow, 1) // last deal
    depthCellAttr.setBackground(bgColor, dealRow, 1) // last deal

    val tickerRow = Array(
      sdf.format(lastTradeTime),
      String.format("%5.2f", Array(snapshotTicker(Ticker.LAST_PRICE))),
      if (prevTicker == null) "-" else currentSize
    )
    tickerModel.insertRow(0, tickerRow.asInstanceOf[Array[Object]])

    if (prevTicker == null) {
      prevTicker = new Ticker
    }
    prevTicker.copy(snapshotTicker)

    repaint()
  }

  private def showCell(table: JTable, row: Int, column: Int) {
    val rect = table.getCellRect(row, column, true)
    table.scrollRectToVisible(rect)
    table.clearSelection
    table.setRowSelectionInterval(row, row)
    /* notify the model */
    table.getModel.asInstanceOf[DefaultTableModel].fireTableDataChanged
  }

  class TrendSensitiveCellRenderer extends DefaultTableCellRenderer {

    this.setForeground(Color.BLACK)
    this.setBackground(LookFeel.getCurrent.backgroundColor)
    this.setOpaque(true)


    override def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                               hasFocus: Boolean, row: Int, column: Int): Component = {

      /** Beacuse this will be a sinleton for all cells, so, should clear it first */
      this.setForeground(Color.BLACK)
      this.setBackground(LookFeel.getCurrent.backgroundColor)
      this.setText(null)

      if (value != null) {
        column match {
          case 0 => // Time
            this.setHorizontalAlignment(SwingConstants.LEADING)
          case 1 => // Price
            this.setHorizontalAlignment(SwingConstants.TRAILING)
            if (row + 1 < table.getRowCount) {
              try {
                var floatValue = NUMBER_FORMAT.parse(value.toString.trim).floatValue
                val prevValue = table.getValueAt(row + 1, column)
                if (prevValue != null) {
                  val prevFloatValue = NUMBER_FORMAT.parse(prevValue.toString.trim).floatValue
                  if (floatValue > prevFloatValue) {
                    this.setForeground(Color.WHITE)
                    this.setBackground(LookFeel.getCurrent.getPositiveBgColor)
                  } else if (floatValue < prevFloatValue) {
                    this.setForeground(Color.WHITE)
                    this.setBackground(LookFeel.getCurrent.getNegativeBgColor)
                  } else {
                    this.setForeground(Color.BLACK)
                    this.setBackground(LookFeel.getCurrent.getNeutralBgColor)
                  }
                }
              } catch {case ex: ParseException => ex.printStackTrace}
            }
          case 2 => // Size
            this.setHorizontalAlignment(SwingConstants.TRAILING)
        }
        this.setText(value.toString)
      }

      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
      this
    }
  }

  def getChartViewContainer: ChartViewContainer = {
    viewContainer
  }

  private def test {
    tickerModel.addRow(Array("00:01", "12334", "1").asInstanceOf[Array[Object]])
    tickerModel.addRow(Array("00:02", "12333", "1234").asInstanceOf[Array[Object]])
    tickerModel.addRow(Array("00:03", "12335", "12345").asInstanceOf[Array[Object]])
    tickerModel.addRow(Array("00:04", "12334", "123").asInstanceOf[Array[Object]])
    tickerModel.addRow(Array("00:05", "12334", "123").asInstanceOf[Array[Object]])
    showCell(tickerTable, tickerTable.getRowCount - 1, 0)
  }
}

object ValueCell {
  def setRowColumn(modelData: Array[Array[Object]]) {
    for (i <- 0 until modelData.length) {
      val rows = modelData(i)
      for (j <- 0 until rows.length) {
        rows(j) match {
          case cell: ValueCell =>
            cell.row = i
            cell.column = j
        }
      }
    }
  }
}
class ValueCell(var row: Int, var column: Int) {

  var value: String

  def this() = this(0, 0)

  override def toString = {
    value
  }

}
