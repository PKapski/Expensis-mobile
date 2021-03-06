package pl.polsl.expensis_mobile.activities.expenses

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import com.android.volley.Request
import com.android.volley.VolleyError
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.expense_details_activity.*
import kotlinx.android.synthetic.main.expenses_activity.*
import org.json.JSONArray
import org.json.JSONObject
import pl.polsl.expensis_mobile.R
import pl.polsl.expensis_mobile.activities.expenses.ExpenseDetailsActivity.RequestType.*
import pl.polsl.expensis_mobile.adapters.SpinnerAdapter
import pl.polsl.expensis_mobile.dto.ExpenseDTO
import pl.polsl.expensis_mobile.dto.ExpenseFormDTO
import pl.polsl.expensis_mobile.models.Category
import pl.polsl.expensis_mobile.models.Expense
import pl.polsl.expensis_mobile.others.DecimalDigitsInputFilter
import pl.polsl.expensis_mobile.others.LoadingAction
import pl.polsl.expensis_mobile.rest.*
import pl.polsl.expensis_mobile.utils.*
import pl.polsl.expensis_mobile.validators.ExpenseValidator
import java.util.*

class ExpenseDetailsActivity : AppCompatActivity(), LoadingAction {

    private var editMode: Boolean = false
    private lateinit var expense: Expense
    private lateinit var editableSpinnerBackground: Drawable
    private lateinit var editableTextViewBackground: Drawable
    private lateinit var expenseJsonObject: JSONObject

    private enum class RequestType {
        FETCH_CATEGORIES,
        DELETE_EXPENSE,
        PUT_EXPENSE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.expense_details_activity)
        expense =
            intent.extras?.get(IntentKeys.EXPENSE_DETAIL) as Expense
        fillTextViewValues()
        showProgressBar()
        fetchCategoriesCallback()
        editableSpinnerBackground = expenseDetailsCategory.background
        editableTextViewBackground = expenseDetailsTitle.background
        changeEditableFields(false)
        expenseDetailsValue.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(6, 2))
    }

    private fun fillTextViewValues() {
        expenseDetailsTitle.setText(expense.title)
        expenseDetailsDescription.setText(expense.description)
        expenseDetailsDate.text = expense.date.toString()
        expenseDetailsValue.setText(expense.value.toString())
    }

    override fun showProgressBar() {
        Thread {
            this.runOnUiThread {
                expenseProgressBar.visibility = View.VISIBLE
            }
        }.start()
    }

    fun hideProgressBar() {
        expenseProgressBar.visibility = View.INVISIBLE
    }

    override fun changeEditableFields(isEnabled: Boolean) {
        expenseDetailsTitle.isClickable = isEnabled
        expenseDetailsTitle.isFocusableInTouchMode = isEnabled
        expenseDetailsTitle.background = if (isEnabled) editableTextViewBackground else null
        if (!isEnabled) expenseDetailsTitle.updatePadding(left = 0)
        expenseDetailsDescription.isFocusableInTouchMode = isEnabled
        expenseDetailsDescription.isClickable = isEnabled
        expenseDetailsDescription.background = if (isEnabled) editableTextViewBackground else null
        expenseDetailsDescription.hint =
            if (isEnabled) applicationContext.getString(R.string.description) else ""
        if (!isEnabled) expenseDetailsDescription.updatePadding(left = 0)
        expenseDetailsDate.isClickable = isEnabled
        expenseDetailsDate.background = if (isEnabled) editableTextViewBackground else null
        if (!isEnabled) expenseDetailsDate.updatePadding(left = 0)
        expenseDetailsCategory.background = if (isEnabled) editableSpinnerBackground else null
        expenseDetailsCategory.isEnabled = isEnabled
        expenseDetailsValue.isClickable = isEnabled
        expenseDetailsValue.isFocusableInTouchMode = isEnabled
        expenseDetailsValue.background = if (isEnabled) editableTextViewBackground else null
        if (!isEnabled) expenseDetailsValue.updatePadding(left = 0)
    }

    fun onGoBackClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        startExpensesActivity()
    }


    fun onDeleteClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        showDeleteConfirmationDialog()
    }

    private fun fetchCategoriesCallback() {
        fetchCategories(getFetchCategoriesCallback())
    }

    private fun getFetchCategoriesCallback(): ServerCallback<JSONArray> {
        return object : ServerCallback<JSONArray> {
            override fun onSuccess(response: JSONArray) {
                val type = object : TypeToken<List<Category>>() {}.type
                val categories = Gson().fromJson<List<Category>>(response.toString(), type)
                fillCategorySpinner(categories)
                hideProgressBar()
            }

            override fun onFailure(error: VolleyError) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                    refreshTokenCallback(FETCH_CATEGORIES)
                } else {
                    val serverError = ServerErrorResponse(error)
                    val messageError = serverError.getErrorResponse()
                    expensesProgressBar.visibility = View.INVISIBLE
                    errorAction(messageError)
                }
            }
        }
    }

    private fun fillCategorySpinner(categories: List<Category>) {
        val items = categories.toMutableList()
        items.add(Category(0, applicationContext.getString(R.string.category_add)))
        val adapter = SpinnerAdapter(
            this,
            R.layout.spinner_category_no_logo_layout,
            R.id.categorySpinnerNoLogoTextView,
            items.toList()
        )
        adapter.setDropDownViewResource(R.layout.spinner_category_no_logo_layout)
        expenseDetailsCategory.adapter = adapter

        var categoryIndex = 0
        for (i in categories.indices) {
            if (categories[i].value == expense.category) {
                categoryIndex = i
                break
            }
        }

        expenseDetailsCategory.setSelection(categoryIndex)
    }

    private fun refreshTokenCallback(requestType: RequestType) {
        TokenUtils.refreshToken(object : ServerCallback<JSONObject> {
            override fun onSuccess(response: JSONObject) {
                SharedPreferencesUtils.storeTokens(
                    response.get(SharedPreferencesUtils.accessTokenConst) as String,
                    TokenUtils.refreshToken,
                    null
                )
                when (requestType) {
                    FETCH_CATEGORIES -> {
                        fetchCategories(getFetchCategoriesCallback())
                    }
                    DELETE_EXPENSE -> {
                        requestDeleteExpense(getDeleteExpenseCallback())
                    }
                    PUT_EXPENSE -> {
                        putExpense(getPutExpenseCallback())
                    }
                }
            }

            override fun onFailure(error: VolleyError) {
                TokenUtils.refreshTokenOnFailure(error)
            }
        })
    }

    private fun fetchCategories(callback: ServerCallback<JSONArray>) {
        val url = BASE_URL + Endpoint.CATEGORIES
        VolleyService().requestArray(Request.Method.GET, url, null, callback, this)
    }

    private fun errorAction(messageError: String?) {
        val intent = Intent(applicationContext, ExpensesActivity::class.java)
        showToast(messageError)
        startActivity(intent)
    }

    private fun showToast(messageError: String?) {
        Toast.makeText(this, messageError, Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmationDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Delete confirmation")
        dialogBuilder.setMessage("Are you sure you wanna delete this expense?")
        dialogBuilder.setPositiveButton("Yes") { _, _ ->
            deleteExpense()
        }
        dialogBuilder.setNeutralButton("Cancel") { _, _ -> }
        dialogBuilder.create().show()
    }

    private fun deleteExpense() {
        showProgressBar()
        requestDeleteExpense(getDeleteExpenseCallback())
    }

    private fun getDeleteExpenseCallback(): ServerCallback<JSONObject> {
        return object : ServerCallback<JSONObject> {
            override fun onSuccess(response: JSONObject) {
                startExpensesActivity()
                Toast.makeText(
                    applicationContext, "Expense successfully deleted!",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onFailure(error: VolleyError) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                    refreshTokenCallback(DELETE_EXPENSE)
                } else {
                    Toast.makeText(
                        applicationContext, "Expense deletion failed!",
                        Toast.LENGTH_LONG
                    ).show()
                    hideProgressBar()
                }

            }
        }
    }

    fun startExpensesActivity() {
        startActivity(Intent(this, ExpensesActivity::class.java))
    }

    private fun requestDeleteExpense(callback: ServerCallback<JSONObject>) {
        val url = BASE_URL + Endpoint.EXPENSES + expense.id + '/'
        VolleyService().requestObject(Request.Method.DELETE, url, null, callback, this)
    }

    fun onEditClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        editMode = !editMode
        changeIconAndEditableFields(editMode)
        if (!editMode) {
            currentFocus?.clearFocus()
            saveChangesInDatabase()
        }
    }

    private fun changeIconAndEditableFields(isEditable: Boolean) {
        editExpenseIcon.setBackgroundResource(if (isEditable) R.drawable.ic_apply else R.drawable.ic_edit)
        changeEditableFields(isEditable)
    }

    private fun saveChangesInDatabase() {
        val expenseFormDTO = ExpenseFormDTO(
            expenseDetailsTitle,
            expenseDetailsDescription,
            expenseDetailsDate,
            expenseDetailsCategory,
            expenseDetailsValue
        )
        val expenseValidator = ExpenseValidator(expenseFormDTO)
        val validationResult = expenseValidator.validateAddExpenseAction()
        if (validationResult.isValid) {
            val expenseDTO = ExpenseDTO(expenseFormDTO)
            expenseJsonObject = JSONObject(Utils.getGsonWithLocalDate().toJson(expenseDTO))
            showProgressBar()
            putExpenseCallback()
        } else {
            Toast.makeText(this, validationResult.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun putExpenseCallback() {
        putExpense(getPutExpenseCallback())
    }

    private fun getPutExpenseCallback(): ServerCallback<JSONObject> {
        return object : ServerCallback<JSONObject> {
            override fun onSuccess(response: JSONObject) {
                hideProgressBar()
                showToast(Messages.SUCCESSFULLY_EDITED)
            }

            override fun onFailure(error: VolleyError) {
                if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                    refreshTokenCallback(PUT_EXPENSE)
                } else {
                    val serverResponse = ServerErrorResponse(error)
                    val messageError = serverResponse.getErrorResponse()
                    if (messageError != null)
                        showToast(messageError)
                    changeIconAndEditableFields(true)
                    hideProgressBar()
                }
            }
        }
    }

    private fun putExpense(callback: ServerCallback<JSONObject>) {
        val url = BASE_URL + Endpoint.EXPENSES + expense.id + '/'
        VolleyService().requestObject(Request.Method.PUT, url, expenseJsonObject, callback, this)
    }

    fun onCreationDateClicked(@Suppress("UNUSED_PARAMETER") view: View) {
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                run {
                    val stringDate = Utils.parseDateToString(year, monthOfYear, dayOfMonth)
                    expenseDetailsDate.text = stringDate
                }
            }

        val dateNow = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            R.style.CalendarTheme,
            dateSetListener,
            dateNow.get(Calendar.YEAR),
            dateNow.get(Calendar.MONTH),
            dateNow.get(Calendar.DAY_OF_MONTH)

        )

        dialog.datePicker.maxDate = dateNow.timeInMillis
        dialog.show()
    }
}