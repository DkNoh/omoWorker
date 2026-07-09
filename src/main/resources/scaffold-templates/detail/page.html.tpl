<!DOCTYPE html>
<!-- Scaffold 생성(DETAIL). 생성 후 개발자가 직접 수정해 소유한다. -->
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{defaultLayout}">
<head>
    <title>@@DOMAIN_NAME@@</title>
</head>
<body>
<main layout:fragment="content">

    <div class="content-header">
        <h2>@@DOMAIN_NAME@@</h2>
    </div>

    @@SEARCH_CARD@@

    <div th:replace="~{fragments/toast-grid :: gridCard}"></div>

    <th:block th:replace="~{fragments/modal-base :: layout(
        modalId='@@DOMAIN_ID@@-modal',
        title='@@DOMAIN_NAME@@ 상세',
        size='modal-lg',
        bodyContent=~{::#modal-body},
        footerContent=null
    )}">
        <div id="modal-body">
            <form id="detail-form" class="row g-3" autocomplete="off">
                @@MODAL_BODY@@
            </form>
        </div>
    </th:block>

</main>
<th:block layout:fragment="script">
    <script th:src="@{/js/@@MODULE_NAME@@/@@DOMAIN_ID@@.js}"></script>
</th:block>
</body>
</html>
