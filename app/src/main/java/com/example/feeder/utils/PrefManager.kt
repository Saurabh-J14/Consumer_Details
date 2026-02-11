package com.example.feeder.utils

import android.content.Context
import android.content.SharedPreferences

class PrefManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREF_NAME = "FeederPrefs"

        private const val KEY_IS_USER_LOGGED_IN = "is_user_logged_in"
        private const val KEY_EMPLOYEE_ID = "employee_id"
        private const val KEY_EMPLOYEE_NAME = "employee_name"
        private const val KEY_MOBILE_NUMBER = "mobile_number"
        private const val KEY_EMAIL = "email"
        private const val KEY_ACCESS_TOKEN = "access_token"

    }

    // ----------------- Boolean -----------------
    fun isUserLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_USER_LOGGED_IN, false)
    fun setUserLoggedIn(isLoggedIn: Boolean) = prefs.edit().putBoolean(KEY_IS_USER_LOGGED_IN, isLoggedIn).apply()

    // ----------------- String -----------------
    fun getEmployeeId(): String? = prefs.getString(KEY_EMPLOYEE_ID, null)
    fun setEmployeeId(empId: String?) = prefs.edit().putString(KEY_EMPLOYEE_ID, empId).apply()

    fun getEmployeeName(): String? = prefs.getString(KEY_EMPLOYEE_NAME, null)
    fun setEmployeeName(name: String?) = prefs.edit().putString(KEY_EMPLOYEE_NAME, name).apply()

    fun getMobileNumber(): String? = prefs.getString(KEY_MOBILE_NUMBER, null)
    fun setEmployeeMobile(mobile: String?) = prefs.edit().putString(KEY_MOBILE_NUMBER, mobile).apply()

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun setEmployeeEmail(email: String?) = prefs.edit().putString(KEY_EMAIL, email).apply()

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun setAccessToken(token: String?) = prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()


    // ----------------- Clear Data -----------------
    fun clearUserData() {
        prefs.edit().apply {
            remove(KEY_IS_USER_LOGGED_IN)
            remove(KEY_EMPLOYEE_ID)
            remove(KEY_EMPLOYEE_NAME)
            remove(KEY_MOBILE_NUMBER)
            remove(KEY_EMAIL)
            remove(KEY_ACCESS_TOKEN)
            apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
