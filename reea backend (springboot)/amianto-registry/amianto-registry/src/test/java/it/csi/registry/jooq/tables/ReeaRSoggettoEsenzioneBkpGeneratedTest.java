package it.csi.registry.jooq.tables;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.QueryPart;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.csi.registry.jooq.Reea;

@ExtendWith(MockitoExtension.class)
public class ReeaRSoggettoEsenzioneBkpGeneratedTest {

    @Mock
    private Condition mockCondition;

    @Mock
    private Select<?> mockSelect;

    @Mock
    private Field<Boolean> mockBooleanField;

    @Mock
    private QueryPart mockQueryPart;

    @Test
    public void testGetSchemaForBaseTable() throws Exception {
        ReeaRSoggettoEsenzioneBkp table = new ReeaRSoggettoEsenzioneBkp();
        Schema schema = table.getSchema();

        assertNotNull(schema);
        assertEquals(Reea.REEA, schema);
    }

    @Test
    public void testGetSchemaForAliasedTableString() throws Exception {
        ReeaRSoggettoEsenzioneBkp aliased = new ReeaRSoggettoEsenzioneBkp("alias_soggetto_esenzione_bkp");

        Schema schema = aliased.getSchema();
        assertNull(schema, "Aliased tables should not expose a schema");
    }

    @Test
    public void testGetSchemaForAliasedTableName() throws Exception {
        Name aliasName = DSL.name("alias_name");
        ReeaRSoggettoEsenzioneBkp aliased = new ReeaRSoggettoEsenzioneBkp(aliasName);

        Schema schema = aliased.getSchema();
        assertNull(schema);
    }

    @Test
    public void testStaticInstanceAndFieldsNotNull() throws Exception {
        ReeaRSoggettoEsenzioneBkp table = ReeaRSoggettoEsenzioneBkp.REEA_R_SOGGETTO_ESENZIONE_BKP;

        assertNotNull(table);
        assertNotNull(table.SOGGETTO_ESENZIONE_ID);
        assertNotNull(table.SOGGETTO_ID_HMAC);
        assertNotNull(table.SOGGETTO_ID_CIFRATO);
        assertNotNull(table.ESENZIONE_ID_HMAC);
        assertNotNull(table.ESENZIONE_ID_CIFRATO);
        assertNotNull(table.ESENZIONE_DATA_EMISSIONE);
        assertNotNull(table.ESENZIONE_DATA_SCADENZA);
        assertNotNull(table.VALIDITA_INIZIO);
        assertNotNull(table.VALIDITA_FINE);
        assertNotNull(table.DATA_CREAZIONE);
        assertNotNull(table.DATA_MODIFICA);
        assertNotNull(table.DATA_CANCELLAZIONE);
        assertNotNull(table.UTENTE_CREAZIONE);
        assertNotNull(table.UTENTE_MODIFICA);
        assertNotNull(table.UTENTE_CANCELLAZIONE);
    }

    @Test
    public void testAsWithStringAlias() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();
        ReeaRSoggettoEsenzioneBkp aliased = base.as("my_alias");

        assertNotSame(base, aliased);
        assertEquals(DSL.name("my_alias"), aliased.getQualifiedName());
        assertNull(aliased.getSchema());
    }

    @Test
    public void testAsWithNameAlias() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();
        Name aliasName = DSL.name("alias_with_name");
        ReeaRSoggettoEsenzioneBkp aliased = base.as(aliasName);

        assertNotSame(base, aliased);
        assertEquals(aliasName, aliased.getQualifiedName());
        assertNull(aliased.getSchema());
    }

    @Test
    public void testAsWithTableAlias() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();
        ReeaRSoggettoEsenzioneBkp firstAlias = base.as("first_alias");

        ReeaRSoggettoEsenzioneBkp secondAlias = base.as(firstAlias);

        assertNotSame(base, secondAlias);
        assertEquals(firstAlias.getQualifiedName(), secondAlias.getQualifiedName());
        assertNull(secondAlias.getSchema());
    }

    @Test
    public void testRenameWithStringName() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();
        ReeaRSoggettoEsenzioneBkp renamed = base.rename("renamed_table");

        assertNotSame(base, renamed);
        assertEquals(DSL.name("renamed_table"), renamed.getQualifiedName());
        // A renamed base table should still expose its schema
        assertEquals(Reea.REEA, renamed.getSchema());
    }

    @Test
    public void testRenameWithName() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();
        Name newName = DSL.name("renamed_with_name");
        ReeaRSoggettoEsenzioneBkp renamed = base.rename(newName);

        assertNotSame(base, renamed);
        assertEquals(newName, renamed.getQualifiedName());
        assertEquals(Reea.REEA, renamed.getSchema());
    }

    @Test
    public void testRenameWithTableName() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();
        ReeaRSoggettoEsenzioneBkp alias = base.as("alias_for_rename");

        ReeaRSoggettoEsenzioneBkp renamed = base.rename(alias);

        assertNotSame(base, renamed);
        assertEquals(alias.getQualifiedName(), renamed.getQualifiedName());
        assertEquals(Reea.REEA, renamed.getSchema());
    }

    @Test
    public void testWhereWithSingleConditionOnBaseTable() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.where(mockCondition);

        assertNotSame(base, derived);
        // Derived from base table: not aliased, schema is still available
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereWithSingleConditionOnAliasedTable() throws Exception {
        ReeaRSoggettoEsenzioneBkp aliased = new ReeaRSoggettoEsenzioneBkp("alias_for_where");

        ReeaRSoggettoEsenzioneBkp derived = aliased.where(mockCondition);

        assertNotSame(aliased, derived);
        // Derived from aliased table: schema is null
        assertNull(derived.getSchema());
    }

    @Test
    public void testWhereWithCollectionOfConditions() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.where(Collections.singletonList(mockCondition));

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereWithVarargsConditionsEdgeCaseEmpty() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.where();

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereWithMultipleConditions() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        Condition secondCondition = DSL.trueCondition();
        ReeaRSoggettoEsenzioneBkp derived = base.where(mockCondition, secondCondition);

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereWithBooleanField() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.where(mockBooleanField);

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereWithSQLObject() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();
        SQL sqlCondition = DSL.sql("1 = 1");

        ReeaRSoggettoEsenzioneBkp derived = base.where(sqlCondition);

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereWithPlainStringSQL() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.where("1 = 1");

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereWithStringAndBinds() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.where("soggetto_esenzione_id = ?", 123);

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereWithStringAndQueryParts() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.where("1 = 1", mockQueryPart);

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereExists() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.whereExists(mockSelect);

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testWhereNotExists() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        ReeaRSoggettoEsenzioneBkp derived = base.whereNotExists(mockSelect);

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }

    @Test
    public void testQualifiedNameConsistencyAcrossAliasesAndRenames() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();
        ReeaRSoggettoEsenzioneBkp aliased = base.as("qualified_alias");
        ReeaRSoggettoEsenzioneBkp renamed = base.rename("qualified_rename");

        assertEquals(DSL.name("qualified_alias"), aliased.getQualifiedName());
        assertEquals(DSL.name("qualified_rename"), renamed.getQualifiedName());
        assertNotEquals(aliased.getQualifiedName(), renamed.getQualifiedName());
    }

    @Test
    public void testWhereWithMultipleConditionsUsingCollection() throws Exception {
        ReeaRSoggettoEsenzioneBkp base = new ReeaRSoggettoEsenzioneBkp();

        Condition condition1 = mockCondition;
        Condition condition2 = DSL.falseCondition();

        ReeaRSoggettoEsenzioneBkp derived = base.where(Arrays.asList(condition1, condition2));

        assertNotSame(base, derived);
        assertEquals(Reea.REEA, derived.getSchema());
    }
}
