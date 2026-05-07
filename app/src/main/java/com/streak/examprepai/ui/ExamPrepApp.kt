package com.streak.examprepai.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.streak.examprepai.data.Exam
import com.streak.examprepai.data.PaletteState
import com.streak.examprepai.data.ProgressStats
import com.streak.examprepai.data.PracticeRecommendation
import com.streak.examprepai.data.QuizHistoryItem
import com.streak.examprepai.data.QuestionReviewItem
import com.streak.examprepai.data.QuestionSet
import com.streak.examprepai.data.QuizMode
import com.streak.examprepai.data.QuizSession
import com.streak.examprepai.data.QuizSummary
import com.streak.examprepai.data.ReviewFilter
import com.streak.examprepai.data.StreakInfo
import com.streak.examprepai.data.Subject
import com.streak.examprepai.data.SubjectPerformance
import kotlinx.coroutines.delay

private object Routes {
    const val Onboarding = "onboarding"
    const val Dashboard = "dashboard"
    const val Quiz = "quiz"
    const val Summary = "summary"
}

@Composable
fun ExamPrepApp(viewModel: ExamPrepViewModel) {
    val state by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(state.hasOnboarded, currentRoute) {
        if (state.hasOnboarded && currentRoute == Routes.Onboarding) {
            navController.navigate(Routes.Dashboard) {
                popUpTo(Routes.Onboarding) { inclusive = true }
            }
        }
    }

    LaunchedEffect(state.latestSummary, currentRoute) {
        if (state.latestSummary != null && currentRoute == Routes.Quiz) {
            navController.navigate(Routes.Summary) {
                popUpTo(Routes.Quiz) { inclusive = true }
            }
        }
    }

    LaunchedEffect(currentRoute, state.latestSummary) {
        if (currentRoute == Routes.Dashboard && state.latestSummary != null) {
            viewModel.clearSummary()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = if (state.hasOnboarded) Routes.Dashboard else Routes.Onboarding
        ) {
            composable(Routes.Onboarding) {
                OnboardingScreen(
                    exams = state.exams,
                    selectedExam = state.selectedExam,
                    selectedSubjectIds = state.selectedSubjectIds,
                    onExamSelected = viewModel::selectExam,
                    onSubjectToggled = viewModel::toggleSubject,
                    onContinue = {
                        viewModel.completeOnboarding()
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.Dashboard) {
                DashboardScreen(
                    examName = state.preferences.examName,
                    subjects = state.preferences.subjectNames,
                    progress = state.progress,
                    recentHistory = state.recentHistory,
                    streakInfo = state.streakInfo,
                    subjectPerformance = state.subjectPerformance,
                    weakAreaRecommendations = state.weakAreaRecommendations,
                    revisionQuestionCounts = state.revisionQuestionCounts,
                    resumableSession = state.resumableSession,
                    questionSets = state.availableSets,
                    onResumeSession = {
                        viewModel.resumeSavedSession()
                        navController.navigate(Routes.Quiz) {
                            launchSingleTop = true
                        }
                    },
                    onStartQuiz = { set, mode ->
                        viewModel.startQuiz(set, mode)
                        navController.navigate(Routes.Quiz) {
                            launchSingleTop = true
                        }
                    },
                    onRetryWeakArea = { set ->
                        viewModel.startQuiz(set, QuizMode.PRACTICE)
                        navController.navigate(Routes.Quiz) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Routes.Quiz) {
                val session = state.activeSession
                if (session == null) {
                    LaunchedEffect(Unit) {
                        if (state.latestSummary != null) {
                            navController.navigate(Routes.Summary) {
                                popUpTo(Routes.Quiz) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.Dashboard) {
                                popUpTo(Routes.Quiz) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                } else {
                    key(session.sessionId) {
                        QuizScreen(
                            session = session,
                            onBack = {
                                viewModel.leaveQuiz()
                                navController.navigate(Routes.Dashboard) {
                                    popUpTo(Routes.Quiz) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onOptionSelected = viewModel::selectOption,
                            onClearResponse = viewModel::clearResponse,
                            onSaveNext = viewModel::saveAndNext,
                            onMarkForReviewAndNext = viewModel::markForReviewAndNext,
                            onPrevious = viewModel::previousQuestion,
                            onJumpToQuestion = viewModel::revisitQuestion,
                            onToggleBookmark = viewModel::toggleBookmark,
                            onSubmitExam = viewModel::submitExam,
                            onFinishPractice = viewModel::finishPractice
                        )
                    }
                }
            }
            composable(Routes.Summary) {
                val summary = state.latestSummary
                if (summary == null) {
                    LaunchedEffect(Unit) {
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(Routes.Summary) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                } else {
                    SummaryScreen(
                        summary = summary,
                        reviewFilter = state.reviewFilter,
                        onFilterChanged = viewModel::setReviewFilter,
                        onBackToDashboard = {
                            navController.navigate(Routes.Dashboard) {
                                popUpTo(Routes.Summary) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingScreen(
    exams: List<Exam>,
    selectedExam: Exam?,
    selectedSubjectIds: Set<String>,
    onExamSelected: (Exam) -> Unit,
    onSubjectToggled: (Subject) -> Unit,
    onContinue: () -> Unit
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeroCard(
                    eyebrow = "MVP 1",
                    title = "Build your exam path",
                    subtitle = "Pick an exam, choose subjects, and unlock a focused practice experience from day one."
                )
            }
            item { SectionTitle("Choose your exam") }
            items(exams) { exam ->
                SelectableExamCard(
                    exam = exam,
                    selected = selectedExam?.id == exam.id,
                    onClick = { onExamSelected(exam) }
                )
            }
            if (selectedExam != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Choose subjects")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            selectedExam.subjects.forEach { subject ->
                                FilterChip(
                                    selected = subject.id in selectedSubjectIds,
                                    onClick = { onSubjectToggled(subject) },
                                    label = { Text(subject.name) }
                                )
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = onContinue,
                        enabled = selectedSubjectIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        Text("Continue to dashboard")
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    examName: String,
    subjects: List<String>,
    progress: ProgressStats,
    recentHistory: List<QuizHistoryItem>,
    streakInfo: StreakInfo,
    subjectPerformance: List<SubjectPerformance>,
    weakAreaRecommendations: List<PracticeRecommendation>,
    revisionQuestionCounts: Map<String, Int>,
    resumableSession: QuizSession?,
    questionSets: List<QuestionSet>,
    onResumeSession: () -> Unit,
    onStartQuiz: (QuestionSet, QuizMode) -> Unit,
    onRetryWeakArea: (QuestionSet) -> Unit
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeroCard(
                    eyebrow = examName.ifBlank { "ExamPrepAI" },
                    title = "Practice today. Improve tomorrow.",
                    subtitle = if (subjects.isEmpty()) {
                        "Your dashboard will grow with every attempt."
                    } else {
                        "Focused on ${subjects.joinToString()}."
                    }
                )
            }
            item { StreakCard(streakInfo = streakInfo) }
            item { ProgressSection(progress = progress) }
            if (subjectPerformance.isNotEmpty()) {
                item { SubjectPerformanceSection(subjectPerformance = subjectPerformance) }
            }
            if (resumableSession != null) {
                item {
                    ResumeSessionCard(
                        session = resumableSession,
                        onResume = onResumeSession
                    )
                }
            }
            if (weakAreaRecommendations.isNotEmpty()) {
                item {
                    WeakAreasSection(
                        recommendations = weakAreaRecommendations,
                        onRetry = onRetryWeakArea
                    )
                }
            }
            item { RecentHistorySection(history = recentHistory) }
            item { SectionTitle("Question sets") }
            if (questionSets.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No question sets yet",
                        description = "Pick at least one subject during onboarding to unlock your first practice sets."
                    )
                }
            } else {
                items(questionSets) { set ->
                    QuestionSetCard(
                        questionSet = set,
                        revisionCount = revisionQuestionCounts[set.id] ?: 0,
                        onStartQuiz = onStartQuiz
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectPerformanceSection(subjectPerformance: List<SubjectPerformance>) {
    val strongest = subjectPerformance.maxByOrNull { it.accuracy }
    val weakest = subjectPerformance.minByOrNull { it.accuracy }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Subject performance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (strongest != null && weakest != null) {
                Text(
                    text = if (strongest.subjectId == weakest.subjectId) {
                        "${strongest.subjectName} is your only tracked subject so far at ${strongest.accuracy}% accuracy."
                    } else {
                        "Strongest: ${strongest.subjectName} (${strongest.accuracy}%) • Needs work: ${weakest.subjectName} (${weakest.accuracy}%)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            subjectPerformance.forEach { performance ->
                SubjectPerformanceRow(performance = performance)
            }
        }
    }
}

@Composable
private fun SubjectPerformanceRow(performance: SubjectPerformance) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(performance.subjectName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Attempts ${performance.attempts} • Correct ${performance.correct} • Wrong ${performance.wrong} • Last ${performance.latestAccuracy}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "${performance.accuracy}%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                performance.accuracy >= 75 -> MaterialTheme.colorScheme.secondary
                performance.accuracy >= 50 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun StreakCard(streakInfo: StreakInfo) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Daily streak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Current", "${streakInfo.currentDays}d", Modifier.weight(1f))
                StatCard("Best", "${streakInfo.longestDays}d", Modifier.weight(1f))
            }
            Text(
                text = if (streakInfo.practicedToday) {
                    "You have already practiced today. Keep the streak alive tomorrow."
                } else if (streakInfo.currentDays > 0) {
                    "Practice once today to protect your ${streakInfo.currentDays}-day streak."
                } else {
                    "Start a streak today with one completed session."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeakAreasSection(
    recommendations: List<PracticeRecommendation>,
    onRetry: (QuestionSet) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Practice next")
        recommendations.forEach { recommendation ->
            WeakAreaCard(
                recommendation = recommendation,
                onRetry = { onRetry(recommendation.questionSet) }
            )
        }
    }
}

@Composable
private fun WeakAreaCard(
    recommendation: PracticeRecommendation,
    onRetry: () -> Unit
) {
    val questionSet = recommendation.questionSet
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(questionSet.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Last accuracy ${recommendation.latestAccuracy}% • ${questionSet.difficulty}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (questionSet.isPremium) {
                    PremiumBadge()
                }
            }
            Text(
                "Wrong ${recommendation.wrongAnswers} • Marked ${recommendation.markedForReview} • Attempted ${recommendation.attempts}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (questionSet.isPremium) {
                    "This set showed recent struggle, but it is still premium locked."
                } else {
                    "This is one of your weakest recent sets. Replaying it in practice mode should help close the gap quickly."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (questionSet.isPremium) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Premium unlock coming soon")
                }
            } else {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Practice again")
                }
            }
        }
    }
}

@Composable
private fun ResumeSessionCard(
    session: QuizSession,
    onResume: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Resume where you left off", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${historyModeLabel(session.mode)} • ${session.set.title}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Question ${session.currentIndex + 1} of ${session.set.questions.size} • Answered ${session.answeredCount} • Marked ${session.markedCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Resume session")
            }
        }
    }
}

@Composable
private fun RecentHistorySection(history: List<QuizHistoryItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Recent activity")
        if (history.isEmpty()) {
            EmptyStateCard(
                title = "No sessions completed yet",
                description = "Finish a practice or exam session and your recent results will show up here."
            )
        } else {
            history.forEach { item ->
                RecentHistoryCard(item = item)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizScreen(
    session: QuizSession,
    onBack: () -> Unit,
    onOptionSelected: (Int) -> Unit,
    onClearResponse: () -> Unit,
    onSaveNext: () -> Unit,
    onMarkForReviewAndNext: () -> Unit,
    onPrevious: () -> Unit,
    onJumpToQuestion: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    onSubmitExam: (Int) -> Unit,
    onFinishPractice: () -> Unit
) {
    var showSubmitDialog by rememberSaveable(session.sessionId) { mutableStateOf(false) }
    var remainingSeconds by rememberSaveable(session.sessionId) { mutableStateOf(session.set.estimatedMinutes * 60) }
    val isExamMode = session.mode == QuizMode.EXAM
    val progress = session.currentProgress
    val question = session.currentQuestion
    val showExplanation = (session.mode == QuizMode.PRACTICE || session.mode == QuizMode.REVISION) && progress.isLocked

    if (isExamMode) {
        LaunchedEffect(remainingSeconds) {
            if (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds -= 1
            } else {
                onSubmitExam(session.set.estimatedMinutes * 60)
            }
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit exam?") },
            text = {
                Text(
                    "Answered: ${session.answeredCount} | Not answered: ${session.skippedCount} | Marked: ${session.markedCount} | Not visited: ${session.notVisitedCount}"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        onSubmitExam((session.set.estimatedMinutes * 60) - remainingSeconds)
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSubmitDialog = false }) {
                    Text("Continue exam")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            BottomControlBar(
                session = session,
                isFirstQuestion = session.currentIndex == 0,
                isLastQuestion = session.currentIndex == session.set.questions.lastIndex,
                onPrevious = onPrevious,
                onClearResponse = onClearResponse,
                onSaveNext = onSaveNext,
                onMarkForReviewAndNext = onMarkForReviewAndNext,
                onFinishPractice = onFinishPractice,
                onOpenSubmitDialog = { showSubmitDialog = true }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                QuizHeader(
                    session = session,
                    remainingSeconds = remainingSeconds,
                    onBack = onBack
                )
            }
            item {
                if (isExamMode) {
                    PaletteCard(
                        session = session,
                        onJumpToQuestion = onJumpToQuestion
                    )
                } else {
                    PracticeScoreCard(
                        correct = session.correctCount,
                        answered = session.answeredCount,
                        total = session.set.questions.size,
                        bookmarked = session.bookmarkedCount,
                        isRevisionMode = session.mode == QuizMode.REVISION
                    )
                }
            }
            item {
                QuestionCard(
                    session = session,
                    onOptionSelected = onOptionSelected,
                    onToggleBookmark = onToggleBookmark
                )
            }
            if (showExplanation) {
                item {
                    ExplanationCard(
                        explanation = question.explanation,
                        reference = question.reference,
                        commonMistake = if (progress.isTimedOut && progress.selectedOptionIndex == null) {
                            "Time ran out on this question. ${question.commonMistake}"
                        } else {
                            question.commonMistake
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomControlBar(
    session: QuizSession,
    isFirstQuestion: Boolean,
    isLastQuestion: Boolean,
    onPrevious: () -> Unit,
    onClearResponse: () -> Unit,
    onSaveNext: () -> Unit,
    onMarkForReviewAndNext: () -> Unit,
    onFinishPractice: () -> Unit,
    onOpenSubmitDialog: () -> Unit
) {
    val isExamMode = session.mode == QuizMode.EXAM
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isExamMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous")
                }
                OutlinedButton(
                    onClick = onClearResponse,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Response")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSaveNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save & Next")
                }
                Button(
                    onClick = onMarkForReviewAndNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Mark & Next")
                }
            }
            Button(
                onClick = onOpenSubmitDialog,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Previous")
                }
                OutlinedButton(
                    onClick = onSaveNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Next")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onFinishPractice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish")
                }
            }
        }
    }
}

@Composable
private fun QuizHeader(
    session: QuizSession,
    remainingSeconds: Int,
    onBack: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(session.set.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when (session.mode) {
                        QuizMode.EXAM -> "Exam Mode"
                        QuizMode.PRACTICE -> "Practice Mode"
                        QuizMode.REVISION -> "Revision Mode"
                        else -> "Practice"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Q ${session.currentIndex + 1} / ${session.set.questions.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (session.mode == QuizMode.EXAM) {
                val initialSeconds = session.set.estimatedMinutes * 60
                val timerColor = when {
                    remainingSeconds <= (initialSeconds * 0.10f).toInt() -> MaterialTheme.colorScheme.error
                    remainingSeconds <= (initialSeconds * 0.20f).toInt() -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                Text(
                    text = formatDuration(remainingSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    color = timerColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "${session.correctCount}/${session.answeredCount} correct",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun PracticeScoreCard(
    correct: Int,
    answered: Int,
    total: Int,
    bookmarked: Int,
    isRevisionMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("$correct/$answered correct so far", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                if (isRevisionMode) {
                    "Revision mode loads your previously wrong or bookmarked questions. Bookmarked: $bookmarked | Total questions: $total"
                } else {
                    "Free navigation is enabled. Bookmarked: $bookmarked | Total questions: $total"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteCard(
    session: QuizSession,
    onJumpToQuestion: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Question palette", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                session.set.questions.forEachIndexed { index, question ->
                    val progress = session.questionProgress[question.id]
                    val paletteState = progress?.paletteState ?: PaletteState.NOT_VISITED
                    val isCurrent = index == session.currentIndex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(paletteColor(paletteState, isCurrent))
                            .clickable { onJumpToQuestion(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = paletteTextColor(paletteState, isCurrent)
                        )
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaletteLegend("Answered", PaletteState.ANSWERED)
                PaletteLegend("Answered + Marked", PaletteState.ANSWERED_AND_MARKED)
                PaletteLegend("Not Answered", PaletteState.NOT_ANSWERED)
                PaletteLegend("Marked", PaletteState.MARKED)
                PaletteLegend("Not Visited", PaletteState.NOT_VISITED)
            }
            Text(
                "Answered: ${session.answeredOnlyCount} | Answered + Marked: ${session.answeredAndMarkedCount} | Not Answered: ${session.notAnsweredCount} | Marked: ${session.markedOnlyCount} | Not Visited: ${session.notVisitedCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaletteLegend(label: String, state: PaletteState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(paletteColor(state, false))
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun QuestionCard(
    session: QuizSession,
    onOptionSelected: (Int) -> Unit,
    onToggleBookmark: () -> Unit
) {
    val question = session.currentQuestion
    val progress = session.currentProgress
    val isPracticeLocked = (session.mode == QuizMode.PRACTICE || session.mode == QuizMode.REVISION) && progress.isLocked

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Question ${session.currentIndex + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(question.prompt, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                BookmarkRibbonButton(
                    selected = progress.isBookmarked,
                    onClick = onToggleBookmark
                )
            }
            question.options.forEachIndexed { index, option ->
                OptionCard(
                    text = option,
                    selected = progress.selectedOptionIndex == index,
                    isCorrect = isPracticeLocked && question.correctOptionIndex == index,
                    isWrongSelection = isPracticeLocked &&
                        progress.selectedOptionIndex == index &&
                        progress.selectedOptionIndex != question.correctOptionIndex,
                    onClick = {
                        if (!isPracticeLocked || progress.selectedOptionIndex == null) {
                            onOptionSelected(index)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BookmarkRibbonButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    val fillColor = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
    val strokeColor = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    val scale by animateFloatAsState(if (selected) 1.2f else 1.0f, label = "bookmark_scale")
    val stateDescription = if (selected) "Saved to bookmarks" else "Not saved"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .semantics {
                this.stateDescription = stateDescription
            }
            .clickable(
                onClickLabel = if (selected) "Remove bookmark" else "Add bookmark",
                role = Role.Button,
                onClick = onClick
            )
            .graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Canvas(modifier = Modifier.size(width = 24.dp, height = 30.dp)) {
            val ribbonPath = Path().apply {
                moveTo(size.width * 0.2f, size.height * 0.08f)
                lineTo(size.width * 0.8f, size.height * 0.08f)
                lineTo(size.width * 0.8f, size.height * 0.92f)
                lineTo(size.width * 0.5f, size.height * 0.72f)
                lineTo(size.width * 0.2f, size.height * 0.92f)
                close()
            }
            drawPath(
                path = ribbonPath,
                color = fillColor,
                style = if (selected) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = 3f)
            )
            if (!selected) {
                drawLine(
                    color = strokeColor,
                    start = Offset(size.width * 0.2f, size.height * 0.08f),
                    end = Offset(size.width * 0.8f, size.height * 0.08f),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
        Text(
            text = if (selected) "Saved" else "Save",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryScreen(
    summary: QuizSummary,
    reviewFilter: ReviewFilter,
    onFilterChanged: (ReviewFilter) -> Unit,
    onBackToDashboard: () -> Unit
) {
    val filteredItems = summary.reviewItems.filter { item ->
        when (reviewFilter) {
            ReviewFilter.ALL -> true
            ReviewFilter.CORRECT -> item.isCorrect && !item.isSkipped
            ReviewFilter.INCORRECT -> !item.isCorrect && !item.isSkipped
            ReviewFilter.SKIPPED -> item.isSkipped
            ReviewFilter.MARKED -> item.isMarkedForReview
        }
    }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                HeroCard(
                    eyebrow = if (summary.mode == QuizMode.EXAM) "Result & Review" else "Practice Review",
                    title = summary.setTitle,
                    subtitle = "Score ${summary.score} | Accuracy ${summary.accuracy}% | Time ${formatDuration(summary.timeTakenSeconds)}"
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Total",
                        value = summary.totalQuestions.toString(),
                        modifier = Modifier.weight(1f),
                        selected = reviewFilter == ReviewFilter.ALL,
                        onClick = { onFilterChanged(ReviewFilter.ALL) }
                    )
                    StatCard("Attempted", summary.attempted.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Correct",
                        value = summary.correct.toString(),
                        modifier = Modifier.weight(1f),
                        selected = reviewFilter == ReviewFilter.CORRECT,
                        onClick = { onFilterChanged(ReviewFilter.CORRECT) }
                    )
                    StatCard(
                        label = "Incorrect",
                        value = summary.wrong.toString(),
                        modifier = Modifier.weight(1f),
                        selected = reviewFilter == ReviewFilter.INCORRECT,
                        onClick = { onFilterChanged(ReviewFilter.INCORRECT) }
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        label = "Skipped",
                        value = summary.skipped.toString(),
                        modifier = Modifier.weight(1f),
                        selected = reviewFilter == ReviewFilter.SKIPPED,
                        onClick = { onFilterChanged(ReviewFilter.SKIPPED) }
                    )
                    StatCard(
                        label = "Marked",
                        value = summary.markedForReview.toString(),
                        modifier = Modifier.weight(1f),
                        selected = reviewFilter == ReviewFilter.MARKED,
                        onClick = { onFilterChanged(ReviewFilter.MARKED) }
                    )
                }
            }
            item {
                Text(
                    text = if (reviewFilter == ReviewFilter.ALL) {
                        "Tap Correct, Incorrect, Skipped, or Marked to filter the review list."
                    } else {
                        "Showing ${reviewFilterLabel(reviewFilter)} questions. Tap Total to clear the filter."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (filteredItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No questions in this filter",
                        description = when (reviewFilter) {
                            ReviewFilter.ALL -> "There are no review items to show for this session."
                            ReviewFilter.CORRECT -> "No correctly answered questions matched this filter."
                            ReviewFilter.INCORRECT -> "No incorrect questions matched this filter."
                            ReviewFilter.SKIPPED -> "No skipped questions matched this filter."
                            ReviewFilter.MARKED -> "No marked-for-review questions matched this filter."
                        }
                    )
                }
            }
            else {
                items(filteredItems) { item ->
                    ReviewCard(item = item)
                }
            }
            item {
                Button(
                    onClick = onBackToDashboard,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text("Back to dashboard")
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(item: QuestionReviewItem) {
    val statusLabel = when {
        item.isSkipped -> "Skipped"
        item.isCorrect -> "Correct"
        else -> "Incorrect"
    }
    val statusColor = when {
        item.isSkipped -> MaterialTheme.colorScheme.tertiary
        item.isCorrect -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(item.question.prompt, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(statusLabel, style = MaterialTheme.typography.labelLarge, color = statusColor)
            Text(
                "Your answer: ${item.selectedOptionIndex?.let { item.question.options[it] } ?: "Not answered"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Correct answer: ${item.question.options[item.question.correctOptionIndex]}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            if (item.isMarkedForReview) {
                Text("Marked for review", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (item.isBookmarked) {
                Text("Bookmarked", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
            Text("Why it is right", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(item.question.explanation, style = MaterialTheme.typography.bodyMedium)
            Text("Common mistake", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(item.question.commonMistake, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Concept tip", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(item.question.reference, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HeroCard(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SelectableExamCard(
    exam: Exam,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(exam.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(exam.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProgressSection(progress: ProgressStats) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Your progress")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Attempted", progress.attempted.toString(), Modifier.weight(1f))
            StatCard("Correct", progress.correct.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Wrong", progress.wrong.toString(), Modifier.weight(1f))
            StatCard("Accuracy", "${progress.accuracy}%", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Card(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(22.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentHistoryCard(item: QuizHistoryItem) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(item.setTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = historyModeLabel(item.mode),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${item.accuracy}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = "Score ${item.score} • ${item.correct}/${item.totalQuestions} correct • ${formatDuration(item.timeTakenSeconds)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Attempted ${item.attempted} • Wrong ${item.wrong} • Skipped ${item.skipped} • Marked ${item.markedForReview}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuestionSetCard(
    questionSet: QuestionSet,
    revisionCount: Int,
    onStartQuiz: (QuestionSet, QuizMode) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(questionSet.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${questionSet.difficulty} • ${questionSet.questions.size} questions • ${questionSet.estimatedMinutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (questionSet.isPremium) {
                    PremiumBadge()
                }
            }
            Text(
                text = if (questionSet.isPremium) {
                    "Premium sets are visible now so the upgrade path feels native later."
                } else {
                    "Choose a mode: learn with instant feedback, simulate a CBT, or revisit mistakes."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (questionSet.isPremium) {
                OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text("Premium unlock coming soon")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onStartQuiz(questionSet, QuizMode.PRACTICE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Practice")
                    }
                    OutlinedButton(
                        onClick = { onStartQuiz(questionSet, QuizMode.EXAM) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exam")
                    }
                }
                OutlinedButton(
                    onClick = { onStartQuiz(questionSet, QuizMode.REVISION) },
                    enabled = revisionCount > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (revisionCount > 0) "Revision ($revisionCount)" else "Revision")
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PremiumBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text("PREMIUM", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OptionCard(
    text: String,
    selected: Boolean,
    isCorrect: Boolean,
    isWrongSelection: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        isCorrect -> MaterialTheme.colorScheme.secondaryContainer
        isWrongSelection -> MaterialTheme.colorScheme.errorContainer
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isCorrect -> MaterialTheme.colorScheme.onSecondaryContainer
        isWrongSelection -> MaterialTheme.colorScheme.onErrorContainer
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(16.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = contentColor)
    }
}

@Composable
private fun ExplanationCard(
    explanation: String,
    reference: String,
    commonMistake: String
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Explanation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(explanation, style = MaterialTheme.typography.bodyLarge)
            HorizontalDivider()
            Text("Common mistake", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(commonMistake, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Concept tip", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(reference, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun paletteColor(state: PaletteState, isCurrent: Boolean): Color {
    return when {
        isCurrent -> MaterialTheme.colorScheme.primary
        state == PaletteState.ANSWERED -> Color(0xFF2E7D32)
        state == PaletteState.NOT_ANSWERED -> Color(0xFFC62828)
        state == PaletteState.MARKED -> Color(0xFF7E57C2)
        state == PaletteState.ANSWERED_AND_MARKED -> Color(0xFF3949AB)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

@Composable
private fun paletteTextColor(state: PaletteState, isCurrent: Boolean): Color {
    return when {
        isCurrent -> MaterialTheme.colorScheme.onPrimary
        state == PaletteState.NOT_VISITED -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.White
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun historyModeLabel(mode: QuizMode): String {
    return when (mode) {
        QuizMode.PRACTICE -> "Practice"
        QuizMode.EXAM -> "Exam"
        QuizMode.TIMED_PRACTICE -> "Timed Practice"
        QuizMode.SECTIONAL -> "Sectional"
        QuizMode.REVISION -> "Revision"
    }
}

private fun reviewFilterLabel(filter: ReviewFilter): String {
    return when (filter) {
        ReviewFilter.ALL -> "all"
        ReviewFilter.CORRECT -> "correct"
        ReviewFilter.INCORRECT -> "incorrect"
        ReviewFilter.SKIPPED -> "skipped"
        ReviewFilter.MARKED -> "marked"
    }
}
