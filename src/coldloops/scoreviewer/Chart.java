package coldloops.scoreviewer;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.util.*;

public class Chart {

    static CategoryChart makeChart2(List<Osr.TimingDelta> tes) {
        CategoryChart chart = new CategoryChartBuilder()
            .theme(Styler.ChartTheme.Matlab)
            .width(800).height(600)
            .xAxisTitle("delta milliseconds").yAxisTitle("counts").build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setXAxisLabelRotation(90);
        chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setLegendVisible(false);

        TreeMap<Integer, Integer> counts = new TreeMap<>();
        for(Osr.TimingDelta t : tes) {
            if(t.type.equals("MISS")) continue;
            int c = 0;
            if(counts.containsKey(t.delta)) c = counts.get(t.delta);
            c++;
            counts.put(t.delta, c);
        }

        ArrayList<Integer> x = new ArrayList<>();
        ArrayList<Integer> y = new ArrayList<>();
        int min = counts.firstKey();
        int max = counts.lastKey();
        for(int i = min; i <= max; i++) {
            x.add(i);
            if(counts.containsKey(i)) {
                y.add(counts.get(i));
            }else {
                y.add(0);
            }
        }
        chart.addSeries("t", x, y);

        return chart;
    }

    static XYChart makeChart(List<Osr.TimingDelta> tes) {
        XYChart chart = new XYChartBuilder()
            .theme(Styler.ChartTheme.Matlab)
            .width(600).height(500)
            .xAxisTitle("minutes").yAxisTitle("delta milliseconds").build();

        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart.getStyler().setMarkerSize(6);

        TreeMap<String, ArrayList<Double>> x = new TreeMap<>();
        TreeMap<String, ArrayList<Integer>> y = new TreeMap<>();
        for(Osr.TimingDelta t : tes) {
            double minutes = t.curtime/1000d/60;
            if(!x.containsKey(t.type)) x.put(t.type,new ArrayList<Double>());
            if(!y.containsKey(t.type)) y.put(t.type,new ArrayList<Integer>());
            x.get(t.type).add(minutes);
            y.get(t.type).add(t.delta);
        }

        String [] order = new String [] {"SN","LN","MISS"};
        for(String t : order) {
            if(x.containsKey(t))
                chart.addSeries(t, x.get(t), y.get(t));
        }
        return chart;
    }
}
