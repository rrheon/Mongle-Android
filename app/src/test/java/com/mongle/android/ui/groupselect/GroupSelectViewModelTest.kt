package com.mongle.android.ui.groupselect

import com.mongle.android.data.remote.ApiFamilyRepository
import com.mongle.android.data.remote.ApiUserRepository
import com.mongle.android.domain.model.FamilyRole
import com.mongle.android.domain.model.MongleGroup
import com.mongle.android.domain.model.User
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class GroupSelectViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var familyRepository: ApiFamilyRepository
    private lateinit var userRepository: ApiUserRepository
    private lateinit var viewModel: GroupSelectViewModel

    private val mockUser = User(
        id = UUID.randomUUID(),
        email = "test@mongle.com",
        name = "테스트",
        profileImageUrl = null,
        role = FamilyRole.FATHER,
        createdAt = Date()
    )

    private val mockGroup = MongleGroup(
        id = UUID.randomUUID(),
        name = "테스트 가족",
        memberIds = emptyList(),
        createdBy = UUID.randomUUID(),
        createdAt = Date(),
        inviteCode = "ABCDEFGH",
        groupProgressId = UUID.randomUUID()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        familyRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        viewModel = GroupSelectViewModel(familyRepository, userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - loadGroups

    @Test
    fun `loadGroups 성공 시 그룹 목록이 설정된다`() = runTest {
        coEvery { familyRepository.getMyFamilies() } returns listOf(mockGroup)

        viewModel.loadGroups()

        assertEquals(1, viewModel.uiState.value.groups.size)
        assertEquals(mockGroup.name, viewModel.uiState.value.groups[0].name)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadGroups 실패 시 빈 목록으로 처리된다`() = runTest {
        coEvery { familyRepository.getMyFamilies() } throws Exception("네트워크 오류")

        viewModel.loadGroups()

        assertTrue(viewModel.uiState.value.groups.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // MARK: - 입력값 변경

    @Test
    fun `onGroupNameChanged 호출 시 groupName이 업데이트된다`() {
        viewModel.onGroupNameChanged("우리 가족")

        assertEquals("우리 가족", viewModel.uiState.value.groupName)
        assertFalse(viewModel.uiState.value.groupNameError)
    }

    @Test
    fun `onNicknameChanged 호출 시 nickname이 업데이트된다`() {
        viewModel.onNicknameChanged("아빠")

        assertEquals("아빠", viewModel.uiState.value.nickname)
        assertFalse(viewModel.uiState.value.nicknameError)
    }

    @Test
    fun `onJoinCodeChanged 호출 시 joinCode가 업데이트된다`() {
        viewModel.onJoinCodeChanged("ABCDEFGH")

        assertEquals("ABCDEFGH", viewModel.uiState.value.joinCode)
        assertFalse(viewModel.uiState.value.joinCodeError)
    }

    // MARK: - createGroup 유효성 검사

    @Test
    fun `createGroup 시 그룹 수가 3개 이상이면 showMaxGroupsAlert가 true가 된다`() {
        val threeGroups = List(3) { mockGroup.copy(id = UUID.randomUUID()) }
        // 3개 그룹 로드된 상태 시뮬레이션
        viewModel.onGroupNameChanged("새 그룹")
        viewModel.onNicknameChanged("아빠")

        // groups를 직접 설정할 수 없어 loadGroups로 설정
        coEvery { familyRepository.getMyFamilies() } returns threeGroups
        viewModel.loadGroups()

        viewModel.createGroup(onCreated = {})

        assertTrue(viewModel.uiState.value.showMaxGroupsAlert)
    }

    @Test
    fun `createGroup 시 그룹명이 빈 경우 groupNameError가 true가 된다`() {
        viewModel.onGroupNameChanged("")
        viewModel.onNicknameChanged("아빠")

        viewModel.createGroup(onCreated = {})

        assertTrue(viewModel.uiState.value.groupNameError)
        assertFalse(viewModel.uiState.value.nicknameError)
    }

    @Test
    fun `createGroup 시 닉네임이 빈 경우 nicknameError가 true가 된다`() {
        viewModel.onGroupNameChanged("우리 가족")
        viewModel.onNicknameChanged("")

        viewModel.createGroup(onCreated = {})

        assertFalse(viewModel.uiState.value.groupNameError)
        assertTrue(viewModel.uiState.value.nicknameError)
    }

    @Test
    fun `createGroup 성공 시 step이 CREATED로 변경된다`() = runTest {
        viewModel.onGroupNameChanged("우리 가족")
        viewModel.onNicknameChanged("아빠")
        coEvery { familyRepository.createFamily(any(), any()) } returns mockGroup
        coEvery { userRepository.getMe() } returns mockUser
        coEvery { userRepository.update(any()) } returns mockUser

        viewModel.createGroup(onCreated = {})

        assertEquals(GroupSelectStep.NOTIFICATION_PERMISSION, viewModel.uiState.value.step)
        assertEquals(mockGroup.inviteCode, viewModel.uiState.value.inviteCode)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `createGroup 실패 시 errorMessage가 설정된다`() = runTest {
        viewModel.onGroupNameChanged("우리 가족")
        viewModel.onNicknameChanged("아빠")
        coEvery { familyRepository.createFamily(any(), any()) } throws Exception("생성 실패")

        viewModel.createGroup(onCreated = {})

        assertEquals("생성 실패", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `createGroup 실패 메시지에 최대 포함 시 showMaxGroupsAlert가 true가 된다`() = runTest {
        viewModel.onGroupNameChanged("우리 가족")
        viewModel.onNicknameChanged("아빠")
        coEvery { familyRepository.createFamily(any(), any()) } throws Exception("그룹은 최대 3개까지 참여할 수 있습니다.")

        viewModel.createGroup(onCreated = {})

        assertTrue(viewModel.uiState.value.showMaxGroupsAlert)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // MARK: - joinWithCode 유효성 검사

    @Test
    fun `joinWithCode 시 코드가 빈 경우 joinCodeError가 true가 된다`() {
        viewModel.onJoinCodeChanged("")
        viewModel.onNicknameChanged("엄마")

        viewModel.joinWithCode(onJoined = {})

        assertTrue(viewModel.uiState.value.joinCodeError)
        assertFalse(viewModel.uiState.value.nicknameError)
    }

    @Test
    fun `joinWithCode 성공 시 isLoading이 false가 된다`() = runTest {
        viewModel.onJoinCodeChanged("ABCDEFGH")
        viewModel.onNicknameChanged("엄마")
        coEvery { familyRepository.joinFamily(any(), any()) } returns mockGroup

        var joined = false
        viewModel.joinWithCode(onJoined = { joined = true })

        assertTrue(joined)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `joinWithCode 시 유효하지 않은 코드 에러는 한국어 메시지를 설정한다`() = runTest {
        viewModel.onJoinCodeChanged("INVALID1")
        viewModel.onNicknameChanged("엄마")
        coEvery { familyRepository.joinFamily(any(), any()) } throws Exception("유효하지 않은 초대 코드입니다.")

        viewModel.joinWithCode(onJoined = {})

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("유효하지 않은"))
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `joinWithCode 시 코드를 대문자로 변환해 API를 호출한다`() = runTest {
        viewModel.onJoinCodeChanged("abcdefgh")
        viewModel.onNicknameChanged("엄마")
        coEvery { familyRepository.joinFamily("ABCDEFGH", FamilyRole.OTHER) } returns mockGroup

        viewModel.joinWithCode(onJoined = {})

        // joinFamily가 대문자 코드로 호출됐는지 검증
        io.mockk.coVerify { familyRepository.joinFamily("ABCDEFGH", FamilyRole.OTHER) }
    }

    // MARK: - 화면 전환

    @Test
    fun `goToCreate 호출 시 step이 CREATE가 된다`() {
        viewModel.goToCreate()

        assertEquals(GroupSelectStep.CREATE, viewModel.uiState.value.step)
    }

    @Test
    fun `goToJoin 호출 시 step이 JOIN이 되고 prefill 코드가 설정된다`() {
        viewModel.goToJoin("PREFILL1")

        assertEquals(GroupSelectStep.JOIN, viewModel.uiState.value.step)
        assertEquals("PREFILL1", viewModel.uiState.value.joinCode)
    }

    @Test
    fun `goBack 호출 시 SELECT 단계로 돌아가고 폼이 초기화된다`() {
        viewModel.onGroupNameChanged("이름")
        viewModel.onNicknameChanged("닉네임")
        viewModel.goToCreate()

        viewModel.goBack()

        assertEquals(GroupSelectStep.SELECT, viewModel.uiState.value.step)
        assertEquals("", viewModel.uiState.value.groupName)
        assertEquals("", viewModel.uiState.value.nickname)
        assertFalse(viewModel.uiState.value.groupNameError)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `dismissMaxGroupsAlert 호출 시 showMaxGroupsAlert가 false가 된다`() {
        coEvery { familyRepository.getMyFamilies() } returns List(3) { mockGroup.copy(id = UUID.randomUUID()) }
        viewModel.loadGroups()
        viewModel.onGroupNameChanged("이름")
        viewModel.onNicknameChanged("닉")
        viewModel.createGroup(onCreated = {})
        // 최대 그룹 경고가 true인 상태

        viewModel.dismissMaxGroupsAlert()

        assertFalse(viewModel.uiState.value.showMaxGroupsAlert)
    }
}
