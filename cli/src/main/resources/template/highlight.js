import { codeToHtml } from 'https://esm.sh/shiki@3.0.0'


for (let element of document.querySelectorAll("code")) {
    const codeHtml = await codeToHtml(element.textContent, {
        lang: element.className.substring(9),
        theme: 'catppuccin-mocha'
    })

    const newElement = document.createElement("null")
    newElement.innerHTML = codeHtml
    element.parentElement.replaceWith(newElement.firstChild)
}
