async function detectCountry() {
    const phoneNumber = document.getElementById('phoneNumber').value.trim();
    const resultDiv = document.getElementById('result');
    
    if (!phoneNumber) {
        showResult('Please enter a phone number', false);
        return;
    }

    try {
        const response = await fetch('/api/phone/country', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',  // Изменяем на JSON
                'Accept': 'application/json'         // Указываем, что ждем JSON
            },
            body: JSON.stringify({ phoneNumber })    // Отправляем объект как JSON
        });

        const data = await response.json();          // Парсим JSON-ответ

        if (response.ok) {
            showResult(`Country: ${data.country}`, true);  // Извлекаем поле country
        } else {
            showResult(data.message, false);              // Извлекаем сообщение об ошибке
        }
    } catch (error) {
        showResult('Error connecting to server', false);
        console.error('Error:', error);
    }
}

function showResult(message, isSuccess) {
    const resultDiv = document.getElementById('result');
    resultDiv.textContent = message;
    resultDiv.className = `result ${isSuccess ? 'success' : 'error'}`;
}

document.getElementById('phoneNumber').addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        detectCountry();
    }
});