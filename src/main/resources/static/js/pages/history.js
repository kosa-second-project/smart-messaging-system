$(function() {
    const $form = $("#historySearchForm");

    $form.find("[data-auto-submit]").on("change", function() {
        $form.find("input[name='page']").remove();
        $form.trigger("submit");
    });

    $form.find("[data-tag-submit]").on("change", function() {
        $form.find("input[name='page']").remove();
        $form.trigger("submit");
    });
});
