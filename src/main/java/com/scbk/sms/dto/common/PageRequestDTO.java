package com.scbk.sms.dto.common;

/** 목록 조회 공통 페이지 요청. 도메인별 검색 DTO는 이 클래스를 상속한다. */
public class PageRequestDTO {

  private int page = 1;
  private int size = 10;
  private String keyword;
  private String searchType;

  public int getOffset() {
    return (page - 1) * size;
  }

  public void validate() {
    if (page < 1) {
      page = 1;
    }
    if (size < 1) {
      size = 10;
    }
    if (size > 100) {
      size = 100;
    }
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getKeyword() {
    return keyword;
  }

  public void setKeyword(String keyword) {
    this.keyword = keyword;
  }

  public String getSearchType() {
    return searchType;
  }

  public void setSearchType(String searchType) {
    this.searchType = searchType;
  }
}
