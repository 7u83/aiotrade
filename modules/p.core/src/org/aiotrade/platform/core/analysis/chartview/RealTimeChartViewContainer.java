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
package org.aiotrade.platform.core.analysis.chartview;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import org.aiotrade.lib.indicator.IndicatorDescriptor;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.charting.view.ChartingController;
import org.aiotrade.lib.math.timeseries.QuoteSer;
import org.aiotrade.lib.math.timeseries.computable.Indicator;
import org.aiotrade.lib.indicator.VOLIndicator;
import org.aiotrade.lib.util.swing.GBC;

/**
 *
 * @author Caoyuan Deng
 */
public class RealTimeChartViewContainer extends ChartViewContainer {

    private static List<RealTimeChartViewContainer> INSTANCES = new ArrayList();

    @Override
    public void init(Component focusableParent, ChartingController controller) {
        super.init(focusableParent, controller);

        //getController().setOnCalendarMode(false);
        //getController().growWBar(-2);

        /**
         * if there has been other RealtimeChartViewContainer opened, try to make
         * them having the same isOnCalendarTime and wBar.
         */
        for (RealTimeChartViewContainer c : INSTANCES) {
            getController().setOnCalendarMode(c.getController().isOnCalendarMode());

            float othersWBar = c.getController().getWBar();
            /** which idx is this othersWBar?, find it: */
            //                int othersWBarIdx = wBarIdx;
            //                for (int i = 0; i < BAR_WIDTH_POOL.length; i++) {
            //                    if (BAR_WIDTH_POOL[i] == othersWBar) {
            //                        othersWBarIdx = i;
            //                        break;
            //                    }
            //                }
            //
            //                if (othersWBarIdx != wBarIdx) {
            //                    growWBar(othersWBarIdx - wBarIdx);
            //                }
            break;
        }

        /** add this to container at last */
        INSTANCES.add(this);
    }

    protected void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GBC.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 100;
        gbc.weighty = 618;
        QuoteSer quoteSer = (QuoteSer) getController().getMasterSer();
        quoteSer.setShortDescription(getController().getContents().getUniSymbol());
        setMasterView(new RealTimeQuoteChartView(getController(), quoteSer), gbc);

        IndicatorDescriptor volDescriptor = createVolIndicatorDecsriptor();
        Indicator volIndicator = volDescriptor.getServiceInstance(getController().getMasterSer());
        volIndicator.computeFrom(0);
        gbc.fill = GBC.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 100;
        gbc.weighty = 382;
        addSlaveView(volDescriptor, volIndicator, gbc);
    }

    private IndicatorDescriptor createVolIndicatorDecsriptor() {
        getController().getMasterSer().getFreq();
        IndicatorDescriptor indicator = new IndicatorDescriptor();
        indicator.setActive(true);
        indicator.setServiceClassName(VOLIndicator.class.getName());
        indicator.setFreq(getController().getMasterSer().getFreq());
        return indicator;
    }
}
