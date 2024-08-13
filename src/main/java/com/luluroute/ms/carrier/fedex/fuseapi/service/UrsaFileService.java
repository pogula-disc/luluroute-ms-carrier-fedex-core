package com.luluroute.ms.carrier.fedex.fuseapi.service;

import static com.luluroute.ms.carrier.fedex.util.Constants.TEMP_LOCATION;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luluroute.ms.carrier.config.FileStorageConfig;
import com.luluroute.ms.carrier.config.S3Configuration;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UrsaFileService {

	@Autowired
	FileStorageConfig fileStorageConfig;

	@Autowired
	S3Configuration s3Configuration;

	@Autowired
	AmazonS3 amazonS3;

	public File getFile(String fileName, String fileLocation) {
		log.info("getting File from S3: " + fileName);

		GetObjectRequest getObjectRequest = new GetObjectRequest((s3Configuration.getBucketName()), fileLocation);
		S3Object object = amazonS3.getObject(getObjectRequest);
		String tempFileLocation = new StringBuilder(TEMP_LOCATION).append(fileStorageConfig.getDirectory())
				.append(File.separator).append(fileName).toString();
		log.debug("URSA file saved location>> " + tempFileLocation);
		this.outputFilePath(tempFileLocation);
		try {
			log.info("S3Object: " + new ObjectMapper().writeValueAsString(object.getObjectMetadata()));
			FileUtils.copyInputStreamToFile(object.getObjectContent(), new File(tempFileLocation));
			log.info("File downloaded and copied");
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		log.info("file downloaded successfully");
		return new File(tempFileLocation);
	}

	private void outputFilePath(String tempFileLocation) {
		Path filePath = Paths.get(tempFileLocation);
		final Path tmp = filePath.getParent();
		log.debug("Output file path {}", tmp);
		if (tmp != null) {
			try {
				Files.createDirectories(tmp);
			} catch (IOException e) {
				log.error("failed to create location >>" + e);
			}
		}
	}

}
