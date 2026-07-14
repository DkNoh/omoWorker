<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.scbk.sms.mapper.[( ${model.moduleName()} )].[( ${model.domainClass()} )]Mapper">

    <!-- Scaffold 생성(v1). 생성 후 개발자가 직접 수정해 소유한다. -->
    <!-- 화면 DatePicker 검색값은 YYYYMMDD 문자열로 전달된다 (TuiPageBuilder 규약).
         비교 컬럼이 DATE/TIMESTAMP면 TO_DATE/TO_TIMESTAMP로 감싸고,
         단일 날짜 = 조건은 당일 00:00:00 이상, 다음날 00:00:00 미만 범위로 변환한다. -->

    <sql id="searchConditions">
        <where>
[# th:each="condition : ${xml.searchConditions()}"][# th:if="${condition.conditional()}"]            <if test="[( ${condition.test()} )]">
                [( ${condition.sql()} )]
            </if>
[/][# th:unless="${condition.conditional()}"]            [( ${condition.sql()} )]
[/][/]        </where>
    </sql>

    <sql id="baseQuery">
[# th:each="line : ${xml.baseQueryLines()}"]            [( ${line} )]
[/]    </sql>

    <select id="count" resultType="int">
        /* [( ${model.domainClass()} )]Mapper.count */
        SELECT COUNT(1) FROM (
        <include refid="baseQuery"/>
        ) A
        <include refid="searchConditions"/>
    </select>

    <select id="selectList" resultType="com.scbk.sms.vo.[( ${model.moduleName()} )].[( ${model.domainClass()} )]VO">
        /* [( ${model.domainClass()} )]Mapper.selectList */
        SELECT A.*
        FROM (
        <include refid="baseQuery"/>
        ) A
        <include refid="searchConditions"/>
        ORDER BY [( ${model.orderBy()} )]
        [( ${model.dialect().pageClause()} )]
    </select>
[# th:if="${model.includeCreateUpdate()}"]
    <insert id="insert">
        /* [( ${model.domainClass()} )]Mapper.insert */
        INSERT INTO [( ${model.targetTable()} )] (
[# th:each="value, iter : ${xml.insertValues()}"]            [( ${value.column()} )][# th:if="${!iter.last}"],[/]
[/]        ) VALUES (
[# th:each="value, iter : ${xml.insertValues()}"]            [( ${value.expression()} )][# th:if="${!iter.last}"],[/]
[/]        )
    </insert>

    <!-- update 기준: 수정 허용 컬럼만 SET하고, 잠금 컬럼을 지정한 경우 WHERE에 함께 둔다. -->
    <update id="update">
        /* [( ${model.domainClass()} )]Mapper.update */
        UPDATE [( ${model.targetTable()} )]
[# th:each="assignment, iter : ${xml.updateAssignments()}"][# th:if="${iter.first}"]           SET [/][# th:unless="${iter.first}"]               [/][( ${assignment.column()} )] = [( ${assignment.expression()} )][# th:if="${!iter.last}"],[/]
[/][# th:each="pk, iter : ${xml.pkBindings()}"][# th:if="${iter.first}"]         WHERE [/][# th:unless="${iter.first}"]           AND [/][( ${pk.column()} )] = [( ${pk.expression()} )]
[/][# th:if="${model.hasLockColumn()}"]           AND ([( ${model.lockColumn()} )] = [( ${xml.beforeLockBinding()} )]
                OR ([( ${model.lockColumn()} )] IS NULL AND [( ${xml.beforeLockBinding()} )] IS NULL))
[/]    </update>

    <delete id="delete">
        /* [( ${model.domainClass()} )]Mapper.delete */
        DELETE FROM [( ${model.targetTable()} )][# th:each="pk, iter : ${xml.pkBindings()}"][# th:if="${iter.first}"] WHERE [/][# th:unless="${iter.first}"]           AND [/][( ${pk.column()} )] = [( ${pk.expression()} )]
[/]    </delete>
[/][# th:if="${model.includeExcel()}"]
    <select id="selectListForExcel" resultType="java.util.HashMap">
        /* [( ${model.domainClass()} )]Mapper.selectListForExcel */
        SELECT A.*
        FROM (
        <include refid="baseQuery"/>
        ) A
        <include refid="searchConditions"/>
        ORDER BY [( ${model.orderBy()} )]
    </select>
[/][# th:if="${model.includePrivacy()}"]
    <select id="selectDetail" resultType="com.scbk.sms.vo.[( ${model.moduleName()} )].[( ${model.domainClass()} )]VO">
        /* [( ${model.domainClass()} )]Mapper.selectDetail */
        SELECT A.*
        FROM (
        <include refid="baseQuery"/>
        ) A
        WHERE A.[( ${model.pkColumn()} )] = #{[( ${model.pkFieldName()} )]}
    </select>
[/]
</mapper>
