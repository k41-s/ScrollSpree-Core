(function () {
    'use strict';

    async function addToBasket(button) {
        const product = {
            id: Number(button.dataset.productId),
            name: button.dataset.productName,
            price: Number(button.dataset.productPrice),
            imageId: button.dataset.imageId ? Number(button.dataset.imageId) : null
        };

        const labelEl = button.querySelector('.label');
        const originalLabel = labelEl ? labelEl.textContent : '';

        button.disabled = true;
        if (labelEl) labelEl.textContent = 'Adding…';

        try {
            await ScrollSpreeCart.addToCart(product, 1);

            if (labelEl) labelEl.textContent = 'Added';
            ScrollSpreeCart.showToast(product.name + ' added to your basket.');

            setTimeout(() => {
                if (labelEl) labelEl.textContent = originalLabel;
                button.disabled = false;
            }, 1500);

        } catch (error) {
            console.error('Add to basket failed:', error);
            ScrollSpreeCart.showToast('That did not go through. Try again.');
            if (labelEl) labelEl.textContent = originalLabel;
            button.disabled = false;
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('.add-to-basket').forEach(button => {
            button.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                addToBasket(button);
            });
        });
    });
})();