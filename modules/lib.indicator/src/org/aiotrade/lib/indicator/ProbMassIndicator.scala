TVarTVarTVarTSerTSer/*
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
package org.aiotrade.lib.indicator

import org.aiotrade.lib.math.timeseries.plottable.Plot
import org.aiotrade.lib.math.timeseries.computable.Factor
import org.aiotrade.lib.math.timeseries.TItem
import org.aiotrade.lib.math.timeseries.Ser
import org.aiotrade.lib.math.timeseries.TVar

/**
 *
 * @author Caoyuan Deng
 */
//@IndicatorName("ProbMass")
class ProbMassIndicator(baseSer: Ser) extends SpotIndicator(baseSer) {
  _sname = "Probability Mass"
  _lname = "Probability Mass"
  _overlapping = true
    
  var baseVar: TVar[Float] = _

  val nIntervals = Factor("Number of Intervals", 30.0, 1.0, 1.0, 100.0)
  val period1    = Factor("Period1", 50.0)
  val period2    = Factor("Period2", 100.0)
  val period3    = Factor("Period3", 200.0)
    
    
  val MASS1 = Var[Array[Array[Float]]]("MASS1", Plot.Profile)
  val MASS2 = Var[Array[Array[Float]]]("MASS2", Plot.Profile)
  val MASS3 = Var[Array[Array[Float]]]("MASS3", Plot.Profile)


  def computeSpot(time: Long, masterIdx: Int) :TItem =  {
    val item = createItemOrClearIt(time)
        
    val probability_mass1 = probMass(masterIdx, baseVar, period1, nIntervals)
    val probability_mass2 = probMass(masterIdx, baseVar, period2, nIntervals)
    val probability_mass3 = probMass(masterIdx, baseVar, period3, nIntervals)
        
    item.set(MASS1, probability_mass1)
    item.set(MASS2, probability_mass2)
    item.set(MASS3, probability_mass3)
        
    item
  }

  override def shortDescription: String =  {
    if (baseVar != null) {
      "PM: " + baseVar.name
    } else "PM"
  }
}








