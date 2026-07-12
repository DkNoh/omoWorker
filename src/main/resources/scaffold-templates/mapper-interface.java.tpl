package com.scbk.sms.mapper.@@MODULE_NAME@@;

@@IMPORTS@@
/** Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. */
@Mapper
public interface @@DOMAIN_CLASS@@Mapper {

    int count(@@DOMAIN_CLASS@@SearchRequestDTO request);

    List<@@DOMAIN_CLASS@@VO> selectList(@@DOMAIN_CLASS@@SearchRequestDTO request);
@@DETAIL_METHOD@@@@CRUD_METHODS@@@@EXCEL_METHOD@@}
