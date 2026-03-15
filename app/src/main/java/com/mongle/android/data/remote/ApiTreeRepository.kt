package com.mongle.android.data.remote

import com.mongle.android.domain.model.TreeProgress
import com.mongle.android.domain.model.TreeStage
import com.mongle.android.domain.repository.TreeRepository
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiTreeRepository @Inject constructor(
    private val api: MongleApiService
) : TreeRepository {

    private suspend fun <T> safeCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: HttpException) {
            val msg = e.response()?.errorBody()?.string() ?: e.message()
            throw Exception(msg)
        }
    }

    private fun String.toTreeStage(): TreeStage = when (this) {
        "SEED" -> TreeStage.SEED
        "SPROUT" -> TreeStage.SPROUT
        "SAPLING" -> TreeStage.SAPLING
        "YOUNG_TREE" -> TreeStage.YOUNG_TREE
        "MATURE_TREE" -> TreeStage.MATURE_TREE
        "FLOWERING" -> TreeStage.FLOWERING
        else -> TreeStage.SEED
    }

    private fun TreeProgressResponse.toDomain(): TreeProgress = TreeProgress(
        id = runCatching { UUID.fromString(id) }.getOrElse { UUID.randomUUID() },
        familyId = runCatching { UUID.fromString(familyId) }.getOrElse { UUID.randomUUID() },
        stage = stage.toTreeStage(),
        totalAnswers = totalAnswers,
        consecutiveDays = 0
    )

    override suspend fun getMyTreeProgress(): TreeProgress? = safeCall {
        try {
            api.getTreeProgress().toDomain()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun create(treeProgress: TreeProgress): TreeProgress =
        throw UnsupportedOperationException()

    override suspend fun get(id: UUID): TreeProgress =
        throw UnsupportedOperationException()

    override suspend fun getByFamilyId(familyId: UUID): TreeProgress? = null

    override suspend fun update(treeProgress: TreeProgress): TreeProgress =
        throw UnsupportedOperationException()

    override suspend fun delete(id: UUID) =
        throw UnsupportedOperationException()

    override suspend fun countByStage(stage: TreeStage): Int = 0
}
