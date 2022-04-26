package com.crap.mukluk;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class LineStyleInfo implements Serializable {
	private static final long serialVersionUID = 3731247399806081144L;

	public int lineNumber;
	public int start;
	public int end;
	public ArrayList<Integer> ansiCode;

	public LineStyleInfo() {
		this.lineNumber = -1;
		this.start = -1;
		this.end = -1;
		this.ansiCode = new ArrayList<Integer>();
	}

	public LineStyleInfo(JSONObject jobj) {
		try {
			this.lineNumber = jobj.getInt("lineNumber");
			this.start = jobj.getInt("start");
			this.end = jobj.getInt("end");

			JSONArray jarr = jobj.optJSONArray("ansiCode");
			int len = jarr.length();

			this.ansiCode = new ArrayList<Integer>();

			for (int i = 0; i < len; i++) {
				ansiCode.add(jarr.getInt(i));
			}
		}
		catch (JSONException ex) {
			Log.e("LineStyleInfo", ex.toString());
		}
	}

	public JSONObject asJson() {
		JSONObject jo = new JSONObject();

		try {
			jo.put("lineNumber", lineNumber);
			jo.put("start", start);
			jo.put("end", end);
			jo.put("ansiCode", new JSONArray(ansiCode));
		}
		catch (JSONException ex) {
			Log.e("LineStyleInfo", ex.toString());
		}

		return jo;
	}

	public static JSONArray GetJsonArray(Collection<LineStyleInfo> list) {
		JSONArray jarr = new JSONArray();

		for (LineStyleInfo lsi : list)
			jarr.put(lsi.asJson());

		return jarr;
	}
}
