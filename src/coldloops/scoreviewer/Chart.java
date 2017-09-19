package coldloops.scoreviewer;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import java.util.ArrayList;
import java.util.List;

public class Chart {

    static XYChart makeChart(List<Osr.TimingError> tes) {
        XYChart chart = new XYChartBuilder().width(600).height(500).title("Gaussian Blobs").xAxisTitle("X").yAxisTitle("Y").build();

        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
        chart.getStyler().setChartTitleVisible(false);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart.getStyler().setMarkerSize(10);

        List<Double> x = new ArrayList<>();
        List<Integer> y = new ArrayList<>();
        for(Osr.TimingError t : tes) {
            x.add(t.curtime/1000d/60);
            y.add(t.delta);
        }

        // Series
        chart.addSeries("t", x, y);
        return chart;
    }

    public static void main(String [] args) {
        //XYChart c = makeChart();
        //new SwingWrapper<>(c).displayChart();
    }
}
