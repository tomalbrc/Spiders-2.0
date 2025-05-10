package tcb.spiderstpo.polymer;

import com.google.gson.JsonParseException;
import de.tomalbrc.bil.core.model.Model;
import de.tomalbrc.bil.file.bbmodel.BbElement;
import de.tomalbrc.bil.file.bbmodel.BbModel;
import de.tomalbrc.bil.file.loader.BbModelLoader;
import de.tomalbrc.bil.file.loader.ModelLoader;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class CustomBbModelLoader extends BbModelLoader {
    @Override
    protected void postProcess(BbModel model) {
        super.postProcess(model);

        for (BbElement element : model.elements) {
            if (element.name.endsWith("_e")) {
                element.shade = false;
                element.lightEmission = 15;
            }
        }
    }

    @Override
    public Model load(InputStream input, @NotNull String name) throws JsonParseException {
        try (Reader reader = new InputStreamReader(input)) {
            BbModel model = GSON.fromJson(reader, BbModel.class);
            if (!name.isEmpty()) {
                model.modelIdentifier = name;
            }

            if (model.modelIdentifier == null) {
                model.modelIdentifier = model.name;
            }

            model.modelIdentifier = ModelLoader.normalizedModelId(model.modelIdentifier);
            this.postProcess(model);
            return new CustomBbModelImporter(model).importModel();
        } catch (Throwable throwable) {
            throw new JsonParseException("Failed to parse: " + name, throwable);
        }
    }
}
