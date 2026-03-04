package net.ltxprogrammer.changed.client.renderer.animate.legless;

import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.entity.beast.NagaEntity;
import net.minecraft.client.model.geom.ModelPart;

import java.util.List;

public abstract class AbstractNagaAnimator<T extends ChangedEntity & NagaEntity, M extends AdvancedHumanoidModel<T>> extends HumanoidAnimator.Animator<T, M> {
    public final ModelPart waist;
    public final ModelPart upperAbdomen;
    public final ModelPart lowerAbdomen;
    public final ModelPart tailStart;
    public final List<ModelPart> tailJoints;

    public AbstractNagaAnimator(ModelPart waist, ModelPart upperAbdomen, ModelPart lowerAbdomen, ModelPart tailStart, List<ModelPart> tailJoints) {
        this.waist = waist;
        this.upperAbdomen = upperAbdomen;
        this.lowerAbdomen = lowerAbdomen;
        this.tailStart = tailStart;
        this.tailJoints = tailJoints;
    }
}
