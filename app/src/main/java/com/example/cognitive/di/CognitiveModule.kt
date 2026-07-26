package com.example.cognitive.di

import com.example.cognitive.pipeline.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CognitiveModule {

    @Provides
    @Singleton
    fun provideReasoningCache(): ReasoningCache {
        return ReasoningCache()
    }

    @Provides
    @Singleton
    fun provideReasoningTraceStore(memoryDao: com.example.brain.memory.MemoryDao): ReasoningTraceStore {
        return ReasoningTraceStore(memoryDao)
    }

    @Provides
    @Singleton
    fun provideClarificationEngine(nousRepository: com.example.domain.repository.NousRepository): ClarificationEngine {
        return ClarificationEngine(nousRepository)
    }

    @Provides
    @Singleton
    fun provideFuzzyConstraintInterpreter(nousRepository: com.example.domain.repository.NousRepository): FuzzyConstraintInterpreter {
        return FuzzyConstraintInterpreter(nousRepository)
    }

    @Provides
    @Singleton
    fun provideTaskPlanner(nousRepository: com.example.domain.repository.NousRepository): TaskPlanner {
        return TaskPlanner(nousRepository)
    }

    @Provides
    @Singleton
    fun provideSelfCorrector(): SelfCorrector {
        return SelfCorrector()
    }

    @Provides
    @Singleton
    fun provideReasoningEngine(
        nousRepository: com.example.domain.repository.NousRepository,
        selfCorrector: SelfCorrector
    ): ReasoningEngine {
        return ReasoningEngine(nousRepository, selfCorrector)
    }

    @Provides
    @Singleton
    fun provideUncertaintyAwareness(): UncertaintyAwareness {
        return UncertaintyAwareness()
    }

    @Provides
    @Singleton
    fun provideCriteriaAnalyzer(): CriteriaAnalyzer {
        return CriteriaAnalyzer()
    }

    @Provides
    @Singleton
    fun provideRiskAssessor(): RiskAssessor {
        return RiskAssessor()
    }

    @Provides
    @Singleton
    fun providePreferenceAligner(): PreferenceAligner {
        return PreferenceAligner()
    }

    @Provides
    @Singleton
    fun provideDecisionMaker(
        criteriaAnalyzer: CriteriaAnalyzer,
        riskAssessor: RiskAssessor,
        preferenceAligner: PreferenceAligner
    ): DecisionMaker {
        return DecisionMaker(criteriaAnalyzer, riskAssessor, preferenceAligner)
    }

    @Provides
    @Singleton
    fun provideProblemSolver(nousRepository: com.example.domain.repository.NousRepository): ProblemSolver {
        return ProblemSolver(nousRepository)
    }

    @Provides
    @Singleton
    fun provideConfidenceModulator(): ConfidenceModulator {
        return ConfidenceModulator()
    }
}
