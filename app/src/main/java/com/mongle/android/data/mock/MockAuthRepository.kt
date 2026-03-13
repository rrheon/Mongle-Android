package com.mongle.android.data.mock

import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.SocialLoginCredential
import com.mongle.android.domain.model.User
import com.mongle.android.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAuthRepository @Inject constructor() : AuthRepository {

    private var currentUser: User? = null

    private val mockUsers = mutableListOf(
        User(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            email = "test@mongle.com",
            name = "홍길동",
            profileImageUrl = null,
            role = FamilyRole.FATHER,
            createdAt = Date()
        )
    )

    override suspend fun login(email: String, password: String): User {
        delay(800) // 네트워크 지연 시뮬레이션
        val user = mockUsers.firstOrNull { it.email == email }
            ?: throw Exception("사용자를 찾을 수 없습니다.")
        currentUser = user
        return user
    }

    override suspend fun signup(name: String, email: String, password: String, role: FamilyRole): User {
        delay(800)
        if (mockUsers.any { it.email == email }) {
            throw Exception("이미 사용 중인 이메일입니다.")
        }
        val newUser = User(
            id = UUID.randomUUID(),
            email = email,
            name = name,
            profileImageUrl = null,
            role = role,
            createdAt = Date()
        )
        mockUsers.add(newUser)
        currentUser = newUser
        return newUser
    }

    override suspend fun socialLogin(credential: SocialLoginCredential): User {
        delay(1000)
        val name = credential.fields["name"] ?: "소셜 사용자"
        val email = credential.fields["email"] ?: "${credential.providerType.value}_user@mongle.com"
        val existingUser = mockUsers.firstOrNull { it.email == email }
        if (existingUser != null) {
            currentUser = existingUser
            return existingUser
        }
        val newUser = User(
            id = UUID.randomUUID(),
            email = email,
            name = name,
            profileImageUrl = null,
            role = FamilyRole.OTHER,
            createdAt = Date()
        )
        mockUsers.add(newUser)
        currentUser = newUser
        return newUser
    }

    override suspend fun logout() {
        delay(300)
        currentUser = null
    }

    override suspend fun deleteAccount() {
        delay(800)
        currentUser?.let { user ->
            mockUsers.removeAll { it.id == user.id }
        }
        currentUser = null
    }

    override suspend fun getCurrentUser(): User? {
        delay(200)
        return currentUser
    }
}
