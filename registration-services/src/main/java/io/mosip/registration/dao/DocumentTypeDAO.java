package io.mosip.registration.dao;

import io.mosip.registration.entity.DocumentType;

import java.util.List;

/**
 * This class is used to fetch all the Document type related details.
 * 
 * @author Brahmanada Reddy
 *
 */
public interface DocumentTypeDAO {

	/**
	 * This method is used to fetch the document types
	 * 
	 * @return {@link List} of document types
	 */
	List<DocumentType> getDocumentTypes();

	/**
	 * This method is used to fetch all the document types by the given document type name
	 * 
	 * @param docTypeName
	 *             Doc Type Name
	 * @return List of fetched doc types
	 */
	List<DocumentType> getDocTypeByName(String docTypeName);

}
