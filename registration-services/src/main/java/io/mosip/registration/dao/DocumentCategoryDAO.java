package io.mosip.registration.dao;

import io.mosip.registration.entity.DocumentCategory;

import java.util.List;

/**
 * This class is used to fetch all the Document category related data.
 * 
 * @author Brahmnanda Reddy
 *
 */
public interface DocumentCategoryDAO {

	/**
	 * This method is used to fetch the document categories
	 * 
	 * @return List of document categories
	 */
	List<DocumentCategory> getDocumentCategories();

	/**
	 * This method is used to fetch all document categories by language code.
	 * 
	 * @param langCode Language code
	 * @return List of doc categories
	 */
	List<DocumentCategory> getDocumentCategoriesByLangCode(String langCode);

	/**
	 * This method is used to fetch all document categories by doc categeory code.
	 * 
	 * @param docCategeoryCode document categeory code
	 * @param langCode         Language code
	 * @return doc category
	 */
	DocumentCategory getDocumentCategoryByCodeAndByLangCode(String docCategeoryCode, String langCode);

}
