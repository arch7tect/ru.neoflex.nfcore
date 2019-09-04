package org.emfjson.couchdb.client;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class CouchDocument {

	private final CouchClient client;
	private final DB db;
	private final String docName;

	public CouchDocument(CouchClient client, DB db, String docName) {
		this.client = client;
		this.db = db;
		this.docName = docName;
	}

	/**
	 * Returns true if the document is present in this CouchDB instance.
	 * 
	 */
	public boolean exist() {
		JsonNode node = null;
		try {
			node = client.content(db.getName() + "/" + docName);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return node != null && node.has("_id");
	}

	/**
	 * Returns the content of the latest revision of the document
	 *
	 * @return JsonNode
	 * @throws IOException
	 */
	public JsonNode content() throws IOException {
		return client.content(db.getName() + "/" + docName);
	}

	public byte[] contentAsBytes() throws IOException {
		return client.contentAsBytes(db.getName() + "/" + docName);
	}

	/**
	 * Creates a document from a JsonNode object in the CouchDB instance.
	 *
	 * @param data
	 * @return JsonNode
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public JsonNode create(JsonNode data) throws IOException {
		return create(client.mapper.writeValueAsString(data));
	}

	/**
	 * Creates New document from a JsonNode object in the CouchDB instance.
	 *
	 * @param data
	 * @return JsonNode
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public JsonNode createNew(JsonNode data) throws IOException {
		return createNew(client.mapper.writeValueAsString(data));
	}

	/**
	 * Creates a document from a String in the CouchDB instance.
	 *
	 * @param data
	 * @return JsonNode
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public JsonNode create(String data) throws IOException {
		return client.put(db.getName() + "/" + docName, data);
	}

	/**
	 * Creates New a document from a String in the CouchDB instance.
	 *
	 * @param data
	 * @return JsonNode
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public JsonNode createNew(String data) throws IOException {
		return client.post(db.getName() + "/" + docName, data);
	}

	/**
	 * Deletes this document from this database in the CouchDB instance.
	 *
	 * @return JsonNode
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public JsonNode delete() throws IOException {
		return client.delete(db.getName() + "/" + docName);
	}
	
	/**
	 * Deletes this document with this revision in the CouchDB instance.
	 *
	 * @param revision
	 * @return JsonNode
	 * @throws JsonProcessingException
	 * @throws IOException
	 */
	public JsonNode delete(String revision) throws IOException {
		if (revision.contains("=")) {
			revision = revision.split("=")[1];
		}

		return client.delete(db.getName() + "/" + docName + "?rev=" + revision);
	}

	public String getName() {
		return docName;
	}

}
