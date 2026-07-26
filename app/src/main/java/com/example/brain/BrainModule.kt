package com.example.brain

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BrainModule {

    @Binds
    @Singleton
    abstract fun bindSystemOperationExecutor(impl: SystemOperationExecutorImpl): SystemOperationExecutor

    @Binds
    @Singleton
    abstract fun bindPermissionChecker(impl: PermissionCheckerImpl): PermissionChecker

    // Default Rules
    @Binds
    @IntoSet
    abstract fun bindTorchRule(rule: TorchToggleRule): Rule

    @Binds
    @IntoSet
    abstract fun bindVolumeRule(rule: VolumeAdjustRule): Rule

    @Binds
    @IntoSet
    abstract fun bindBrightnessRule(rule: BrightnessAdjustRule): Rule

    @Binds
    @IntoSet
    abstract fun bindMuteRule(rule: MuteRule): Rule

    @Binds
    @IntoSet
    abstract fun bindUnmuteRule(rule: UnmuteRule): Rule

    @Binds
    @IntoSet
    abstract fun bindDndRule(rule: DndToggleRule): Rule

    @Binds
    @IntoSet
    abstract fun bindBatterySaverRule(rule: BatterySaverToggleRule): Rule

    // Default Skills
    @Binds
    @IntoSet
    abstract fun bindCallSkill(skill: CallSkill): BrainSkill

    @Binds
    @IntoSet
    abstract fun bindSmsSkill(skill: SmsSkill): BrainSkill

    @Binds
    @IntoSet
    abstract fun bindOpenAppSkill(skill: OpenAppSkill): BrainSkill

    @Binds
    @IntoSet
    abstract fun bindTorchSkill(skill: TorchSkill): BrainSkill

    @Binds
    @IntoSet
    abstract fun bindVolumeSkill(skill: VolumeSkill): BrainSkill

    @Binds
    @IntoSet
    abstract fun bindBrightnessSkill(skill: BrightnessSkill): BrainSkill
}
