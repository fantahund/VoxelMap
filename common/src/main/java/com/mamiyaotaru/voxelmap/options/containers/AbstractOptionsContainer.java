package com.mamiyaotaru.voxelmap.options.containers;

import com.mamiyaotaru.voxelmap.options.MapPermissionsManager;
import com.mamiyaotaru.voxelmap.options.fields.OptionField;

import java.io.PrintWriter;
import java.util.LinkedHashMap;

public abstract class AbstractOptionsContainer {
    public final LinkedHashMap<String, OptionField<?>> optionByNames = new LinkedHashMap<>();

    public void addOptionField(OptionField<?> field) {
        optionByNames.put(field.getSaveKey(), field);
    }

    public abstract void updateOptionsActive();

    public abstract void updateOptionsAllowed(MapPermissionsManager permissionsManager);

    public void loadLine(String[] keyValue) {
        OptionField<?> field;
        if ((field = optionByNames.get(keyValue[0])) != null) {
            field.set(keyValue[1]);
        }
    }

    public void saveAll(PrintWriter out) {
        for (OptionField<?> field : optionByNames.values()) {
            out.println(field.getSaveKey() + ":" + field.get());
        }
    }
}
