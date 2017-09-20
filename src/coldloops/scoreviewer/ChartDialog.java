package coldloops.scoreviewer;

import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ChartDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JPanel chartPane;
    private JRadioButton rbt_scatter;
    private JRadioButton rbt_histo;

    private List<Osr.TimingDelta> tes;

    public ChartDialog(JFrame parent, File osu, File replay) {
        super(parent, "test", true);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        this.tes = Osr.calcTimingDeltas(osu, replay);

        ButtonGroup rbt_group = new ButtonGroup();
        rbt_group.add(rbt_scatter);
        rbt_group.add(rbt_histo);
        ActionListener rbt_action = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == rbt_histo) {
                    chartPane.removeAll();
                    chartPane.add(new XChartPanel<>(makeHistChart(tes)));
                    chartPane.updateUI();
                } else {
                    chartPane.removeAll();
                    chartPane.add(new XChartPanel<>(makeScatterChart(tes)));
                    chartPane.updateUI();
                }
            }
        };
        rbt_histo.addActionListener(rbt_action);
        rbt_scatter.addActionListener(rbt_action);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                System.exit(0); // TODO: remove this
            }
        });
    }

    void display() {
        chartPane.setLayout(new BoxLayout(chartPane, BoxLayout.PAGE_AXIS));
        chartPane.add(new XChartPanel<>(makeScatterChart(tes)));
        pack();
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private static XYChart makeHistChart(List<Osr.TimingDelta> tes) {
        XYChart chart = new XYChartBuilder()
                .theme(Styler.ChartTheme.Matlab)
                .width(800).height(600)
                .xAxisTitle("delta milliseconds").yAxisTitle("counts").build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setChartTitleVisible(false);
        //chart.getStyler().setXAxisLabelRotation(90);
        //chart.getStyler().setPlotGridLinesVisible(false);
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setMarkerSize(6);

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
        chart.addSeries("t", x, y).setMarker(SeriesMarkers.NONE);

        return chart;
    }

    private static XYChart makeScatterChart(List<Osr.TimingDelta> tes) {
        XYChart chart = new XYChartBuilder()
                .theme(Styler.ChartTheme.Matlab)
                .width(800).height(600)
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
