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
package org.aiotrade.lib.securities.dataserver

import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Ticker


/**
 *
 * most fields' default value should be OK.
 *
 * @author Caoyuan Deng
 */
object TickerContract {
  val folderName = "TickerServers"
}

import TickerContract._
class TickerContract extends DataContract[Ticker, TickerServer] {
  type T = QuoteSer

  val log = Logger.getLogger(this.getClass.getName)
  
  serviceClassName = null //"org.aiotrade.lib.dataserver.yahoo.YahooTickerServer"
  freq = TFreq.ONE_MIN
  refreshable = true

  override def displayName = {
    "Ticker Data Contract[" + srcSymbol + "]"
  }
    
  /**
   * @param none args are needed
   */
  override def createServiceInstance(args: Any*): Option[TickerServer] = {
    lookupServiceTemplate match {
      case Some(x) => x.createNewInstance.asInstanceOf[Option[TickerServer]]
      case None => None
    }
  }

  def lookupServiceTemplate: Option[TickerServer] = {
    val services = PersistenceManager().lookupAllRegisteredServices(classOf[TickerServer], folderName)
    services find {x => x.getClass.getName == serviceClassName} match {
      case None =>
        try {
          log.warning("Cannot find registeredService of QuoteServer in " + services + ", try Class.forName call: serviceClassName=" + serviceClassName)
          Some(Class.forName(serviceClassName).newInstance.asInstanceOf[TickerServer])
        } catch {
          case ex: Exception => log.log(Level.SEVERE, "Cannot class.forName of class: " + serviceClassName, ex); None
        }
      case some => some
    }
  }
        
  /**
   * Ticker contract don't care about freq, so override super
   */
  override def idEquals(serviceClassName: String, freq: TFreq): Boolean = {
    if (this.serviceClassName.equals(serviceClassName)) true
    else false
  }
    
}

