package com.oikoaudio.fire.sequence;

import java.util.HashMap;
import java.util.Map;

import com.oikoaudio.fire.NoteAssign;

public class FunctionInfo { // TODO this has to be a Display info
	public static final Map<NoteAssign, FunctionInfo> INFO1 = new HashMap<>();
	public static final Map<NoteAssign, FunctionInfo> INFO2 = new HashMap<>();

	static {
		INFO1.put(NoteAssign.MUTE_1, new FunctionInfo("Select", "Pad select"));
		INFO1.put(NoteAssign.MUTE_2, new FunctionInfo("Last Step", "Step: set last step"));
		INFO1.put(NoteAssign.MUTE_3,
				new FunctionInfo("Paste sel", "Pad target", "Paste+"));
		INFO1.put(NoteAssign.MUTE_4,
				new FunctionInfo("Delete", "Pad delete"));
		INFO2.put(NoteAssign.MUTE_1, new FunctionInfo("Mute", "Pad mute"));
		INFO2.put(NoteAssign.MUTE_2, new FunctionInfo("Solo", "Pad solo"));
	}

	private final String name;
	private final String detail;
	private final String shiftFunction;

	public FunctionInfo(final String name, final String detail) {
		this(name, detail, null);
	}

	public FunctionInfo(final String name, final String detail, final String shiftFunction) {
		super();
		this.name = name;
		this.detail = detail;
		this.shiftFunction = shiftFunction;
	}

	public String getName(final boolean shift) {
		if (!shift || shiftFunction == null) {
			return name;
		}
		return shiftFunction;
	}

	public String getDetail() {
		return detail;
	}

}
