package it.csi.registry.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jooq.ExecuteContext;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.impl.DefaultExecuteListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlLoggerListener extends DefaultExecuteListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG =
            LoggerFactory.getLogger(SqlLoggerListener.class);

    /**
     * Maschera il secondo parametro delle funzioni pgp_sym_*.
     */
    private static final Pattern PGP_KEY_PATTERN = Pattern.compile(
            "(pgp_sym_(?:encrypt|encrypt_bytea|decrypt|decrypt_bytea)\\s*\\((?:.|\\R)*?,\\s*)'[^']*'",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    


	/**
	 * Maschera il secondo parametro di reea.hmac_soggetto_id.
	 */

	

		private static final Pattern HMAC_KEY_PATTERN =
		    Pattern.compile(
		        "(reea\\.hmac_soggetto_id\\([^)]*,\\s*)'[^']*'",
		        Pattern.CASE_INSENSITIVE);

		


		private static final Pattern TEXT_TO_HMAC_CAST_KEY_PATTERN =
		    Pattern.compile(
		        "(reea\\.text_to_hmac\\s*\\([^,]+,\\s*cast\\s*\\()'[^']*'",
		        Pattern.CASE_INSENSITIVE);



		private static final Pattern HMAC_CAST_KEY_PATTERN = Pattern.compile(
		    "(reea\\.(?:hmac_soggetto_id|text_to_hmac)[\\s\\S]*?,\\s*cast\\s*\\()'[^']*'",
		    Pattern.CASE_INSENSITIVE);


    @Override
    public void renderEnd(ExecuteContext ctx) {

        if (ctx.query() != null) {

            String sql = ctx.query().getSQL(ParamType.INLINED);

            sql = maskSecretKeys(sql);

            LOG.info("""
                    
                    ==================================================================================
                    SQL
                    ==================================================================================
                    
                    {}
                    
                    ==================================================================================
                    """,
                    sql);
        }
    }

    @Override
    public void fetchEnd(ExecuteContext ctx) {

        Result<?> result = ctx.result();

        if (result != null && !result.isEmpty()) {

            LOG.debug("""
                    
                    ==================================================================================
                    RESULT
                    ==================================================================================
                    
                    {}
                    
                    ==================================================================================
                    """,
                    result.format());
        }
    }

    @Override
    public void executeEnd(ExecuteContext ctx) {

        if (ctx.rows() > 0) {

            LOG.info("""
                    
                    ==================================================================================
                    ROWS AFFECTED
                    ==================================================================================
                    
                    {}
                    
                    ==================================================================================
                    """,
                    ctx.rows());
        }
    }

    @Override
    public void exception(ExecuteContext ctx) {

        if (ctx.exception() != null) {

            LOG.error("""
                    
                    ==================================================================================
                    SQL ERROR
                    ==================================================================================
                    
                    {}
                    
                    ==================================================================================
                    """,
                    ctx.exception().getMessage(),
                    ctx.exception());
        }
    }

    private String maskSecretKeys(String sql) {

        if (sql == null || sql.isBlank()) {
            return sql;
        }

        sql = maskPattern(sql, PGP_KEY_PATTERN, "PGP SECRET KEY");
        sql = maskPattern(sql, HMAC_KEY_PATTERN, "HMAC SECRET KEY");
        sql = maskPattern(sql, TEXT_TO_HMAC_CAST_KEY_PATTERN, "HMAC SECRET KEY");
        sql = maskPattern(sql, HMAC_CAST_KEY_PATTERN, "HMAC SECRET KEY");
        return sql;
    }

    private String maskPattern(String sql, Pattern pattern, String mask) {

        Matcher matcher = pattern.matcher(sql);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {

            String replacement = matcher.group(1) + "'" + mask + "'";

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);

        return result.toString();
    }
}