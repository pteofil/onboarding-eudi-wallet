package ro.banking40.team01.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ro.banking40.team01.R
import ro.banking40.team01.onboarding.ManualForm
import ro.banking40.team01.ui.theme.VerifiedStartTheme

/** Manual form, step 1 of 2: personal details and an ID photo (simulated). */
@Composable
fun FormDetailsScreen(form: ManualForm, onChange: (ManualForm) -> Unit, onNext: () -> Unit) {
    StepLayout(title = stringResource(R.string.form_details_title), body = stringResource(R.string.form_prefilled_note)) {
        FormProgress(step = 1)
        FormField(R.string.form_first_name, form.firstName) { onChange(form.copy(firstName = it)) }
        FormField(R.string.form_last_name, form.lastName) { onChange(form.copy(lastName = it)) }
        FormField(R.string.form_birth_date, form.birthDate, KeyboardType.Number) { onChange(form.copy(birthDate = it)) }
        FormField(R.string.form_cnp, form.cnp, KeyboardType.Number) { onChange(form.copy(cnp = it)) }
        FormField(R.string.form_id_document, form.idDocument) { onChange(form.copy(idDocument = it)) }
        Attachment(R.string.form_id_photo, R.string.form_id_photo_file)
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.form_next)) }
    }
}

/** Manual form, step 2 of 2: address, income and payslips (simulated). */
@Composable
fun FormAddressScreen(form: ManualForm, onChange: (ManualForm) -> Unit, onSubmit: () -> Unit) {
    StepLayout(title = stringResource(R.string.form_address_title), body = stringResource(R.string.form_prefilled_note)) {
        FormProgress(step = 2)
        FormField(R.string.form_street, form.street) { onChange(form.copy(street = it)) }
        FormField(R.string.form_city, form.city) { onChange(form.copy(city = it)) }
        FormField(R.string.form_postal_code, form.postalCode, KeyboardType.Number) { onChange(form.copy(postalCode = it)) }
        FormField(R.string.form_county, form.county) { onChange(form.copy(county = it)) }
        FormField(R.string.form_employer, form.employer) { onChange(form.copy(employer = it)) }
        FormField(R.string.form_income, form.monthlyIncome, KeyboardType.Number) { onChange(form.copy(monthlyIncome = it)) }
        Attachment(R.string.form_payslips, R.string.form_payslips_file)
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.form_submit)) }
    }
}

/** The end of the manual path: nothing is verified, so it waits for a person. Contrasts with the wallet path. */
@Composable
fun FormSubmittedScreen(onUseWallet: () -> Unit, onBackToStart: () -> Unit) {
    StepLayout(title = stringResource(R.string.form_submitted_title), body = stringResource(R.string.form_submitted_body)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.form_submitted_wallet_pitch),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp),
            )
        }
        Button(onClick = onUseWallet, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.start_continue_wallet))
        }
        OutlinedButton(onClick = onBackToStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.form_back_to_start))
        }
    }
}

@Composable
private fun FormProgress(step: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.form_step, step, 2),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        LinearProgressIndicator(progress = { step / 2f }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FormField(label: Int, value: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Attachment(label: Int, file: Int) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(file),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text("✓", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun FormDetailsPreview() = VerifiedStartTheme { FormDetailsScreen(ManualForm(), {}, {}) }
