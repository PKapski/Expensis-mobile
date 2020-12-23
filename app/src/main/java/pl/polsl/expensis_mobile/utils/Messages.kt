package pl.polsl.expensis_mobile.utils

class Messages {
    companion object {
        /**
         * Register view
         */
        const val WRONG_DATE_ERROR = "Date of birth is not valid"
        const val WRONG_MONTHLY_LIMIT_ERROR = "Monthly limit is not valid"
        const val WRONG_EMAIL_ERROR = "Email is not valid"
        const val NOT_SELECTED_GENDER_ERROR = "Select gender"
        const val NOT_SELECTED_INCOME_RANGE_ERROR = "Select income range"
        const val NOT_STRONG_PASSWORD_ERROR = "Password required at least:" +
                " 5 characters, 1 digit and 1 special sign (e.g !%#)"
        const val NOT_EQUAL_PASSWORDS_ERROR = "Passwords are not equal"
        const val SUCCESSFULLY_REGISTERED = "Successfully registered"
        const val UNEXPECTED_ERROR = "Unexpected error"

        /**
         * Login view
         */
        const val EMPTY_EMAIL = "Email cannot be empty"
        const val EMPTY_PASSWORD = "Password cannot be empty"

        /**
         * Common
         */
        const val NO_SERVER_CONNECTION = "Failed to connect with server"
    }
}