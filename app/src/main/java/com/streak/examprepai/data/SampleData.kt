package com.streak.examprepai.data

object SampleData {

    val exams: List<Exam> = listOf(
        Exam(
            id = "upsc",
            name = "UPSC CSE",
            tagline = "Current affairs, polity, history, geography",
            subjects = listOf(
                Subject("polity", "Polity"),
                Subject("history", "History"),
                Subject("geography", "Geography"),
                Subject("economy", "Economy")
            )
        ),
        Exam(
            id = "ssc",
            name = "SSC CGL",
            tagline = "Quant, reasoning, English, general awareness",
            subjects = listOf(
                Subject("quant", "Quantitative Aptitude"),
                Subject("reasoning", "Reasoning"),
                Subject("english", "English"),
                Subject("gk", "General Awareness")
            )
        ),
        Exam(
            id = "gate",
            name = "GATE",
            tagline = "Core concepts, problem solving, technical depth",
            subjects = listOf(
                Subject("maths", "Engineering Maths"),
                Subject("algorithms", "Algorithms"),
                Subject("os", "Operating Systems"),
                Subject("networks", "Computer Networks")
            )
        )
    )

    val questionSets: List<QuestionSet> = listOf(
        QuestionSet(
            id = "upsc-polity-foundation",
            title = "Polity Foundation Drill",
            subjectId = "polity",
            difficulty = "Beginner",
            estimatedMinutes = 8,
            isPremium = false,
            questions = listOf(
                Question(
                    id = "pol-1",
                    prompt = "Which part of the Indian Constitution contains Fundamental Rights?",
                    options = listOf("Part II", "Part III", "Part IV", "Part IVA"),
                    correctOptionIndex = 1,
                    explanation = "Fundamental Rights are listed in Part III of the Constitution. They protect individual liberty and place limits on state action.",
                    reference = "Indian Constitution, Part III",
                    commonMistake = "Students often confuse Part III with Part IV, which contains the Directive Principles of State Policy."
                ),
                Question(
                    id = "pol-2",
                    prompt = "Who is known as the chief architect of the Indian Constitution?",
                    options = listOf("Jawaharlal Nehru", "B. R. Ambedkar", "Sardar Patel", "Rajendra Prasad"),
                    correctOptionIndex = 1,
                    explanation = "Dr. B. R. Ambedkar chaired the Drafting Committee and is widely regarded as the chief architect of the Constitution.",
                    reference = "Constituent Assembly records",
                    commonMistake = "Learners sometimes choose Rajendra Prasad because he was President of the Constituent Assembly, not head of the Drafting Committee."
                ),
                Question(
                    id = "pol-3",
                    prompt = "The Right to Constitutional Remedies is associated with which Article?",
                    options = listOf("Article 19", "Article 21", "Article 32", "Article 44"),
                    correctOptionIndex = 2,
                    explanation = "Article 32 gives citizens the right to approach the Supreme Court for enforcement of Fundamental Rights.",
                    reference = "Indian Constitution, Article 32",
                    commonMistake = "Article 21 is often picked because it is a famous rights article, but remedies are specifically protected by Article 32."
                )
            )
        ),
        QuestionSet(
            id = "ssc-reasoning-sprint",
            title = "Reasoning Speed Sprint",
            subjectId = "reasoning",
            difficulty = "Intermediate",
            estimatedMinutes = 10,
            isPremium = false,
            questions = listOf(
                Question(
                    id = "rea-1",
                    prompt = "If CAT is coded as DBU, how is DOG coded using the same pattern?",
                    options = listOf("EPH", "EOG", "FQI", "DNG"),
                    correctOptionIndex = 0,
                    explanation = "Each letter moves one step forward in the alphabet: D becomes E, O becomes P, and G becomes H.",
                    reference = "SSC verbal reasoning pattern practice",
                    commonMistake = "A common error is shifting only some letters instead of applying the same rule to all three."
                ),
                Question(
                    id = "rea-2",
                    prompt = "Find the odd one out: 3, 5, 11, 14, 17",
                    options = listOf("5", "11", "14", "17"),
                    correctOptionIndex = 2,
                    explanation = "3, 5, 11, and 17 are prime numbers, while 14 is not prime.",
                    reference = "Basic number reasoning",
                    commonMistake = "Students rushing on pattern questions sometimes miss the prime-number connection."
                ),
                Question(
                    id = "rea-3",
                    prompt = "A is taller than B, B is taller than C. Who is the shortest?",
                    options = listOf("A", "B", "C", "Cannot be determined"),
                    correctOptionIndex = 2,
                    explanation = "If A > B and B > C, then C is the shortest by transitive comparison.",
                    reference = "SSC analytical reasoning basics",
                    commonMistake = "Some learners overthink simple ordering questions and pick 'cannot be determined' even when the order is complete."
                )
            )
        ),
        QuestionSet(
            id = "gate-algo-deep-dive",
            title = "Algorithms Deep Dive",
            subjectId = "algorithms",
            difficulty = "Advanced",
            estimatedMinutes = 15,
            isPremium = true,
            questions = listOf(
                Question(
                    id = "alg-1",
                    prompt = "What is the average time complexity of quicksort?",
                    options = listOf("O(n)", "O(log n)", "O(n log n)", "O(n^2)"),
                    correctOptionIndex = 2,
                    explanation = "Quicksort has average-case complexity O(n log n) because balanced partitions reduce the problem size recursively.",
                    reference = "Introduction to Algorithms, Quicksort chapter",
                    commonMistake = "Worst-case O(n^2) is a common trap when the question asks for average-case complexity."
                ),
                Question(
                    id = "alg-2",
                    prompt = "Which traversal of a BST returns keys in sorted order?",
                    options = listOf("Preorder", "Inorder", "Postorder", "Level order"),
                    correctOptionIndex = 1,
                    explanation = "Inorder traversal visits left subtree, root, and then right subtree, producing sorted output in a BST.",
                    reference = "Data structures textbook, BST traversals",
                    commonMistake = "Preorder is often selected because it starts at the root, but it does not guarantee sorted output."
                )
            )
        ),
        QuestionSet(
            id = "upsc-history-pyq",
            title = "Modern History PYQ Pack",
            subjectId = "history",
            difficulty = "Intermediate",
            estimatedMinutes = 12,
            isPremium = true,
            questions = listOf(
                Question(
                    id = "his-1",
                    prompt = "The Swadeshi Movement was launched in response to:",
                    options = listOf("Rowlatt Act", "Partition of Bengal", "Simon Commission", "Montagu Declaration"),
                    correctOptionIndex = 1,
                    explanation = "The Swadeshi Movement began in 1905 as a response to the Partition of Bengal ordered by Lord Curzon.",
                    reference = "Spectrum Modern History",
                    commonMistake = "Students sometimes link Swadeshi to later nationalist milestones rather than its specific trigger in 1905."
                ),
                Question(
                    id = "his-2",
                    prompt = "Who gave the slogan 'Do or Die' during the Quit India Movement?",
                    options = listOf("Subhas Chandra Bose", "Mahatma Gandhi", "Jawaharlal Nehru", "Bal Gangadhar Tilak"),
                    correctOptionIndex = 1,
                    explanation = "Mahatma Gandhi gave the 'Do or Die' call during the Quit India Movement in August 1942.",
                    reference = "NCERT Modern India",
                    commonMistake = "Subhas Chandra Bose is a tempting distractor because of his powerful slogans in a different context."
                )
            )
        )
    )
}
