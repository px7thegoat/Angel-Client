package keystrokesmod.manager;

import keystrokesmod.module.Module;
import keystrokesmod.module.impl.clicker.LeftClicker;
import keystrokesmod.module.impl.ghost.FastPlace;
import keystrokesmod.module.impl.player.AutoPlace;
import keystrokesmod.module.impl.player.AutoTool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleManager {
    public static List<Module> modules = new ArrayList<>();

    public void register() {
        this.addModule(new LeftClicker());
        this.addModule(new AutoPlace());
        this.addModule(new FastPlace());
        this.addModule(new AutoTool());
        modules.sort(Comparator.comparing(Module::getName));
    }

    public void addModule(Module m) {
        modules.add(m);
    }

    public static List<Module> getModules() {
        return modules;
    }

    public Module getModule(String moduleName) {
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }
}
