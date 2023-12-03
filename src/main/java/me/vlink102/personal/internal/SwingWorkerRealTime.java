package me.vlink102.personal.internal;

import org.knowm.xchart.*;
import org.knowm.xchart.internal.series.MarkerSeries;
import org.knowm.xchart.style.markers.Marker;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Creates a real-time chart using SwingWorker
 */
public class SwingWorkerRealTime {

    public MySwingWorker mySwingWorker;
    public SwingWrapper<XYChart> sw;
    XYChart chart;

    public MySwingWorker getMySwingWorker() {
        return mySwingWorker;
    }

    public void go() {

        // Create Chart
        chart = new XYChartBuilder().xAxisTitle("Time").yAxisTitle("Evaluation").title("NNUE Evaluation Display").build();
        XYSeries series = chart.addSeries("Evaluation", new double[] { 0 }, new double[] { 0 });
        series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        series.setSmooth(true);
        series.setLineColor(Color.green);
        series.setMarker(SeriesMarkers.CROSS);
        series.setMarkerColor(Color.RED);

        XYSeries newSeries = chart.addSeries("WinChances", new double[] { 0 }, new double[] { 0 });
        newSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Step);
        newSeries.setLineColor(Color.yellow.darker());
        chart.getStyler().setChartBackgroundColor(Color.darkGray);
        chart.getStyler().setLegendBackgroundColor(Color.darkGray);
        chart.getStyler().setChartFontColor(Color.gray);
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setXAxisTicksVisible(false);
        chart.getStyler().setPlotBackgroundColor(Color.gray.darker());
        chart.getStyler().setPlotGridVerticalLinesVisible(false);
        chart.getStyler().setPlotGridLinesColor(Color.gray);
        chart.getStyler().setZoomEnabled(true);
        chart.getStyler().setZoomResetByDoubleClick(true);
        chart.getStyler().setZoomSelectionColor(new Color(128,128,128,128));
        chart.getStyler().setZoomResetByButton(true);
        // Show it
        sw = new SwingWrapper<>(chart);
        JFrame frame = sw.displayChart();
        frame.setTitle("Chess V3 Graphical Analysis");
        frame.setIconImage(FileUtils.getPieces().get(PieceEnum.values()[ThreadLocalRandom.current().nextInt(PieceEnum.values().length)]).getImage());

        mySwingWorker = new MySwingWorker();
        mySwingWorker.execute();
    }

    public class MySwingWorker extends SwingWorker<Boolean, double[]> {

        LinkedList<Double> fifo = new LinkedList<>();

        public MySwingWorker() {
            fifo.add(0.0);
        }

        public void addData(double data) {
            if (fifo.getLast().equals(data)) return;
            fifo.add(data);
        }

        @Override
        protected Boolean doInBackground() {

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (fifo.size() > 100) {
                        fifo.removeFirst();
                    }

                    double[] array = new double[fifo.size()];
                    for (int i = 0; i < fifo.size(); i++) {
                        array[i] = fifo.get(i);
                    }
                    publish(array);

                }
            }, 0, 1000);

            return true;
        }

        @Override
        protected void process(List<double[]> chunks) {

            double[] mostRecentDataSet = chunks.get(chunks.size() - 1);

            chart.updateXYSeries("Evaluation", null, mostRecentDataSet, null);

            double[] mostRecentXSet = new double[mostRecentDataSet.length];
            for (int i = 0; i < mostRecentDataSet.length; i++) {
                double z = StockFish.getWinningChances(mostRecentDataSet[i] * 100d);
                mostRecentXSet[i] = z;
            }
            chart.updateXYSeries("WinChances", null, mostRecentXSet, null);
            sw.repaintChart();
        }
    }
}