package tcb.spiderstpo.polymer;

import de.tomalbrc.bil.core.model.Node;
import de.tomalbrc.bil.file.bbmodel.BbModel;
import de.tomalbrc.bil.file.bbmodel.BbOutliner;
import de.tomalbrc.bil.file.bbmodel.BbTexture;
import de.tomalbrc.bil.file.importer.BbModelImporter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

import java.util.UUID;

public class CustomBbModelImporter extends BbModelImporter {
    public CustomBbModelImporter(BbModel model) {
        super(model);
    }

    @Override
    protected Object2ObjectOpenHashMap<UUID, Node> makeNodeMap() {
        Object2ObjectOpenHashMap<UUID, Node> nodeMap = new Object2ObjectOpenHashMap<>();
        ObjectArraySet<BbTexture> textures = new ObjectArraySet<>();
        textures.addAll(this.model.textures);
        ObjectListIterator<BbOutliner.ChildEntry> iterator = this.model.outliner.iterator();

        while (iterator.hasNext()) {
            BbOutliner.ChildEntry entry = iterator.next();
            if (entry.isNode()) {
                this.createBones(null, null, this.model.outliner, nodeMap);
            }
        }

        // dont write any textures
        //BbResourcePackGenerator.makeTextures(this.model, textures);
        return nodeMap;
    }
}
