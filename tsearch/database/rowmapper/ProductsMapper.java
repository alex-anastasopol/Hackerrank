package ro.cst.tsearch.database.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import ro.cst.tsearch.utils.DBConstants;

public class ProductsMapper implements ParameterizedRowMapper<ProductsMapper> {

	/**
	 * Mapper for Products
	 */
	
	public static final String TABLE_PRODUCTS = "ts_search_products";	
	
		
	private int productId;
	private String name;
	private String alias;
	private int order;
	private String shortName;
		
	@Override
	public ProductsMapper mapRow(ResultSet resultSet, int rowNum) throws SQLException {
		ProductsMapper productsMapper = new ProductsMapper();
		productsMapper.setProductId(resultSet.getInt(DBConstants.FIELD_SEARCH_PRODUCTS_ID));
		productsMapper.setName(resultSet.getString(DBConstants.FIELD_SEARCH_PRODUCTS_NAME));
		productsMapper.setAlias(resultSet.getString(DBConstants.FIELD_SEARCH_PRODUCTS_ALIAS));
		productsMapper.setOrder(resultSet.getInt(DBConstants.FIELD_SEARCH_PRODUCTS_ORDER));
		productsMapper.setShortName(resultSet.getString(DBConstants.FIELD_SEARCH_PRODUCTS_SHORT_NAME));

		return productsMapper;
	}

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

}
