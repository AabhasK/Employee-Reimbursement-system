const type = document.querySelector('#type');
const amount = document.querySelector('#amount');
const numberField = document.querySelector('#receipt-number');
const numberLabel = numberField.querySelector('label');
const description = document.querySelector('#description');
const defaultDescription = description.placeholder;
const fields = document.querySelector('#proof-fields');
const receipt = document.querySelector('#receipt-upload');
const letter = document.querySelector('#letter-upload');
const formats = document.querySelector('#proof-formats');
const rule = document.querySelector('#proof-rule');
const attached = [...fields.querySelectorAll('.attached [data-kind]')];
const hasReceipt = attached.some(doc => doc.dataset.kind !== 'APPROVAL_LETTER');
const hasLetter = attached.some(doc => doc.dataset.kind === 'APPROVAL_LETTER');
const slots = Number(fields.dataset.max) - attached.length;

function showField(row, show) {
    row.hidden = !show;
    for (const input of row.querySelectorAll('input')) {
        input.disabled = !show;
    }
}

function showProof() {
    const option = type.selectedOptions[0];
    const equipment = option.value === 'EQUIPMENT';
    numberLabel.textContent = equipment ? 'Invoice number (required)' : 'Receipt or ticket number (required)';
    description.placeholder = equipment ? 'Monitor, mouse, keyboard or other equipment' : defaultDescription;
    const threshold = option.dataset.proofAbove;
    const enteredAmount = Number(amount.value);
    const needsReceipt = threshold !== undefined && (Number(threshold) === 0 || enteredAmount > Number(threshold));
    const allowsLetter = option.dataset.letter === 'true';
    const receiptFile = receipt.querySelector('input[type=file]');
    const letterFile = letter.querySelector('input[type=file]');

    if (receipt.querySelector('[name=kind1]').value !== option.dataset.kind) {
        receiptFile.value = '';
        letterFile.value = '';
        receipt.querySelector('[name=kind1]').value = option.dataset.kind;
    }
    const showReceipt = needsReceipt && slots > 0 && !(hasReceipt && slots === 1 && letterFile.files.length);
    const reserveReceipt = showReceipt && (!hasReceipt || receiptFile.files.length);
    const showLetter = allowsLetter && !hasLetter && slots > (reserveReceipt ? 1 : 0);
    const label = `${option.dataset.label} (${hasReceipt ? 'optional additional copy' : 'required'})`;
    receipt.querySelector('label').textContent = label;

    if (needsReceipt) {
        rule.textContent = allowsLetter
            ? 'Receipt required. An HoD or Dean approval letter is optional.'
            : 'Receipt required.';
        if (equipment) {
            rule.textContent = 'Purchase invoice required. Include the item name and model in the description.';
        }
    } else if (threshold !== undefined && !(enteredAmount > 0)) {
        rule.textContent = `A receipt is required for claims above ₹${threshold}.`;
    } else {
        rule.textContent = 'No receipt required for this claim.';
    }

    showField(numberField, needsReceipt);
    showField(receipt, showReceipt);
    showField(letter, showLetter);
    formats.hidden = !showReceipt && !showLetter;
    fields.hidden = !showReceipt && !showLetter && attached.length === 0;
}

type.addEventListener('change', showProof);
amount.addEventListener('input', showProof);
receipt.addEventListener('change', showProof);
letter.addEventListener('change', showProof);
showProof();
