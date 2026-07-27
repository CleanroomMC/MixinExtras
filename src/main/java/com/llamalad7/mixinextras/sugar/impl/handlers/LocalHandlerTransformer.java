package com.llamalad7.mixinextras.sugar.impl.handlers;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.service.MixinExtrasService;
import com.llamalad7.mixinextras.sugar.impl.SugarParameter;
import com.llamalad7.mixinextras.sugar.impl.ref.LocalRefUtils;
import com.llamalad7.mixinextras.utils.ASMUtils;
import com.llamalad7.mixinextras.wrapper.factory.FactoryRedirectWrapper;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Ensures all locals are captured by reference for injectors that can participate in a {@link WrapOperation} chain.
 * If a wrapper sets the local, the inner handler must receive the most up-to-date value.
 */
class LocalHandlerTransformer extends HandlerTransformer {
    /**
     * Ours and Mixin's own descriptors only. Another instance's {@link WrapOperation} is translated by
     * {@link #isTargetInjector} on lookup, see the note on
     * {@link com.llamalad7.mixinextras.service.MixinExtrasServiceImpl#ourNameFor}.
     */
    private static final Set<String> TARGET_INJECTORS = new HashSet<>(Arrays.asList(
            Type.getDescriptor(ModifyConstant.class),
            Type.getDescriptor(Redirect.class),
            Type.getDescriptor(FactoryRedirectWrapper.class),
            Type.getDescriptor(WrapOperation.class)
    ));

    LocalHandlerTransformer(IMixinInfo mixin, SugarParameter parameter) {
        super(mixin, parameter);
    }

    @Override
    public boolean isRequired(MethodNode handler) {
        AnnotationNode annotation = InjectionInfo.getInjectorAnnotation(this.mixin, handler);
        return annotation != null && isTargetInjector(annotation.desc) && LocalRefUtils.getTargetType(parameter.type, parameter.genericType) == parameter.type;
    }

    private static boolean isTargetInjector(String desc) {
        if (TARGET_INJECTORS.contains(desc)) {
            return true;
        }
        String ourDesc = MixinExtrasService.getInstance().ourDescriptorFor(desc);
        return ourDesc != null && TARGET_INJECTORS.contains(ourDesc);
    }

    @Override
    public void transform(HandlerInfo info) {
        Type wrapperType = Type.getType(LocalRefUtils.getInterfaceFor(this.parameter.type));
        info.wrapParameter(
                this.parameter,
                wrapperType,
                ASMUtils.isPrimitive(this.parameter.type) ? null : this.parameter.type,
                (insns, load) -> {
                    LocalRefUtils.generateUnwrapping(insns, this.parameter.type, load);
                }
        );
    }
}
