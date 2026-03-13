package com.mongle.android.data.mock

import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.TreeStage
import com.mongle.android.domain.repository.TreeRepository
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockTreeRepository @Inject constructor() : TreeRepository {

    private val mockTree = TreeProgress(
        id = UUID.fromString("00000000-0000-0000-0000-000000000020"),
        familyId = UUID.fromString("00000000-0000-0000-0000-000000000010"),
        stage = TreeStage.YOUNG_TREE,
        totalAnswers = 12,
        consecutiveDays = 5,
        badgeIds = emptyList(),
        lastUpdated = Date()
    )

    override suspend fun create(treeProgress: TreeProgress): TreeProgress {
        delay(300)
        return treeProgress
    }

    override suspend fun get(id: UUID): TreeProgress {
        delay(200)
        return mockTree
    }

    override suspend fun getByFamilyId(familyId: UUID): TreeProgress? {
        delay(200)
        return mockTree
    }

    override suspend fun update(treeProgress: TreeProgress): TreeProgress {
        delay(300)
        return treeProgress
    }

    override suspend fun delete(id: UUID) {
        delay(200)
    }

    override suspend fun countByStage(stage: TreeStage): Int {
        delay(200)
        return if (stage == mockTree.stage) 1 else 0
    }

    override suspend fun getMyTreeProgress(): TreeProgress {
        delay(300)
        return mockTree
    }
}
