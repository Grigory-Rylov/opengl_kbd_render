package com.github.grishberg.cad3d.debug.cmd;


import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;
import java.util.List;
import com.github.grishberg.javascad.coords.V3d;
import java.util.ArrayList;

public class LogCmd implements DebugCmd {

    private final String message;
    private final SplitStartedCmd splitStartedCmd;
	private List<V3d> points = new ArrayList<>();


    public LogCmd(String message, SplitStartedCmd splitStartedCmd) {
        this.message = message;
        this.splitStartedCmd = splitStartedCmd;
    }
	
	public LogCmd(String message, List<V3d> points, SplitStartedCmd splitStartedCmd) {
        this.message = message;
        this.splitStartedCmd = splitStartedCmd;
		this.points = new ArrayList<>(points);
    }

    @Override
    public String getDescription() {
		if(!points.isEmpty()){
			return message + " "+ verticesToString(points);
		}
        return message;
    }
	
	public static String verticesToString(List<V3d> vertices) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vertices.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(vertices.get(i));
        }
        return sb.toString();
    }

    public void render(DebugVisualizer debugVisualizer) {
        if (splitStartedCmd != null) {
            splitStartedCmd.render(debugVisualizer);
        }
    }
}
