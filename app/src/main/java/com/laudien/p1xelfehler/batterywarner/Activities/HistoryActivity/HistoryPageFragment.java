package com.laudien.p1xelfehler.batterywarner.Activities.HistoryActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.Series;
import com.laudien.p1xelfehler.batterywarner.R;

public class HistoryPageFragment extends Fragment {

    private static final String TAG = "HistoryPageFragment";
    private Series[] series;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_page, container, false);
        GraphView graphView = (GraphView) view.findViewById(R.id.graphView);
        for (Series s : series) {
            graphView.addSeries(s);
        }
        Viewport viewport = graphView.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMaxX(series[0].getHighestValueX());
        viewport.setMinX(0);
        viewport.setMaxY(100);
        return view;
    }

    public void addGraphs(Series[] series) {
        this.series = series;
    }
}