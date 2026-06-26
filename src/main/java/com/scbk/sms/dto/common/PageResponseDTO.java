package com.scbk.sms.dto.common;

import java.util.List;

/** 목록 조회 공통 페이지 응답. 생성은 PageResponseDTO.of(list, request, totalCount)만 사용한다. */
public class PageResponseDTO<T> {

  private final List<T> contents;
  private final int page;
  private final int size;
  private final long totalCount;
  private final int totalPages;
  private final boolean hasNext;
  private final boolean hasPrev;

  private PageResponseDTO(
      List<T> contents,
      int page,
      int size,
      long totalCount,
      int totalPages,
      boolean hasNext,
      boolean hasPrev) {
    this.contents = contents;
    this.page = page;
    this.size = size;
    this.totalCount = totalCount;
    this.totalPages = totalPages;
    this.hasNext = hasNext;
    this.hasPrev = hasPrev;
  }

  public static <T> PageResponseDTO<T> of(List<T> contents, PageRequestDTO req, long totalCount) {
    int totalPages = (int) Math.ceil((double) totalCount / req.getSize());
    return new PageResponseDTO<>(
        contents,
        req.getPage(),
        req.getSize(),
        totalCount,
        Math.max(totalPages, 1),
        req.getPage() < totalPages,
        req.getPage() > 1);
  }

  public List<T> getContents() {
    return contents;
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public long getTotalCount() {
    return totalCount;
  }

  public int getTotalPages() {
    return totalPages;
  }

  public boolean isHasNext() {
    return hasNext;
  }

  public boolean isHasPrev() {
    return hasPrev;
  }
}
