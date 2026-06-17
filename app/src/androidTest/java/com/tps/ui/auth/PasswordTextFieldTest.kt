package com.tps.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PasswordTextFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun passwordFieldDefaultsToHiddenAndTogglesVisibility() {
        composeRule.setContent {
            MaterialTheme {
                val text = remember { mutableStateOf("") }
                PasswordTextField(
                    value = text.value,
                    onValueChange = { text.value = it },
                    label = "密码"
                )
            }
        }

        composeRule.onNodeWithContentDescription("显示密码").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("显示密码").performClick()
        composeRule.onNodeWithContentDescription("隐藏密码").assertIsDisplayed()
    }
}
