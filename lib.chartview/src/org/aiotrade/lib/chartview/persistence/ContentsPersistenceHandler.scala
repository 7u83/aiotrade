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
package org.aiotrade.lib.chartview.persistence

import java.text.ParseException
import java.text.SimpleDateFormat
import javax.swing.text.DateFormatter
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.util.serialization.BeansDocument

/**
 * @author Caoyuan Deng
 */

object ContentsPersistenceHandler {
    
  def dumpContents(contents: AnalysisContents): String = {
    val buffer = new StringBuilder(500)
    val beans = new BeansDocument
    beans.appendBean(contents.writeToBean(beans))
        
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    //buffer.append("<!DOCTYPE settings PUBLIC \"-//AIOTrade//DTD AnalysisContents settings 1.0//EN\" >\n")
    buffer.append("<sec unisymbol=\"" + contents.uniSymbol + "\">\n")

    val df = new DateFormatter(new SimpleDateFormat("yyyy-MM-dd"))
    val dataContracts = contents.lookupDescriptors(classOf[QuoteContract])
    if (dataContracts.size > 0) {
      buffer.append("    <sources>\n")
      for (dataContract <- dataContracts) {
        buffer.append("        <source ")
        buffer.append("active=\"" + dataContract.active + "\" ")
        buffer.append("class=\"" + dataContract.serviceClassName + "\" ")
        buffer.append("symbol=\"" + dataContract.symbol + "\" ")
        buffer.append("sectype=\"" + dataContract.secType + "\" ")
        buffer.append("exchange=\"" + dataContract.exchange + "\" ")
        buffer.append("primaryexchange=\"" + dataContract.primaryExchange + "\" ")
        buffer.append("currency=\"" + dataContract.currency + "\" ")
        buffer.append("dateformat=\"" + dataContract.dateFormatPattern + "\" ")
        buffer.append("nunits=\"" + dataContract.freq.nUnits + "\" ")
        buffer.append("unit=\"" + dataContract.freq.unit + "\" ")
        buffer.append("refreshable=\"" + dataContract.refreshable + "\" ")
        buffer.append("refreshinterval=\"" + dataContract.refreshInterval + "\" ")
        try {
          buffer.append("begdate=\"" + df.valueToString(dataContract.beginDate.getTime) + "\" ")
          buffer.append("enddate=\"" + df.valueToString(dataContract.endDate.getTime) + "\" ")
        } catch {case ex: ParseException => ex.printStackTrace}
        buffer.append("url=\"" + dataContract.urlString + "\"")
        buffer.append(">\n")
        buffer.append("        </source>\n")
      }
      buffer.append("    </sources>\n")
    }
        
    val indicatorDescriptors = contents.lookupDescriptors(classOf[IndicatorDescriptor])
    if (indicatorDescriptors.size > 0) {
      buffer.append("    <indicators>\n")
      for (descriptor <- indicatorDescriptors) {
        buffer.append("        <indicator ")
        buffer.append("active=\"" + descriptor.active + "\" ")
        buffer.append("class=\"" + descriptor.serviceClassName + "\" ")
        buffer.append("nunits=\"" + descriptor.freq.nUnits + "\" ")
        buffer.append("unit=\"" + descriptor.freq.unit + "\">\n")
                
        val factors = descriptor.factors
        for (factor <- factors) {
          buffer.append("            <opt name=\"").append(factor.name)
          .append("\" value=\"").append(factor.value)
          .append("\" step=\"").append(factor.step)
          .append("\" minvalue=\"").append(factor.minValue)
          .append("\" maxvalue=\"").append(factor.maxValue)
          .append("\"/>\n")
        }
                
        buffer.append("        </indicator>\n")
      }
      buffer.append("    </indicators>\n")
    }
        
    val drawingDescriptors = contents.lookupDescriptors(classOf[DrawingDescriptor])
    if (drawingDescriptors.size > 0) {
      buffer.append("    <drawings>\n")
      for (descriptor <- drawingDescriptors) {
        buffer.append("        <layer ")
        buffer.append("name=\"" + descriptor.serviceClassName + "\" ")
        buffer.append("nunits=\"" + descriptor.freq.nUnits + "\" ")
        buffer.append("unit=\"" + descriptor.freq.unit + "\">\n ")
        val chartMapPoints = descriptor.getHandledChartMapPoints
        for (chart <- chartMapPoints.keysIterator) {
          buffer.append("            <chart class=\"" + chart.getClass.getName + "\">\n")
          for (point <- chartMapPoints.get(chart).get) {
            buffer.append("                <handle t=\"" + point.t + "\" v=\"" + point.v + "\"/>\n")
          }
          buffer.append("            </chart>\n")
        }
        buffer.append("        </layer>\n")
      }
      buffer.append("    </drawings>\n")
    }
        
    buffer.append("</sec>")
        
    //beans.saveDoc();
        
    return buffer.toString
  }
    
  def loadContents {
  }
    
}
