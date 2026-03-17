package io.mosip.registration.service.bio.impl;

import io.mosip.registration.dto.iso.ImageDetail;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class ISO {

    private int height = 640;
    private int width = 480;


    public  byte[] crtImg(byte[] image){
        ImageDetail imageDetail = new ImageDetail();
        imageDetail.setData(image);
        imageDetail.setHeight(height);
        imageDetail.setWidth(width);
        return getFaceHeaderWithImage(imageDetail);
    }

    public byte[] getFaceHeaderWithImage(ImageDetail imageDetail) {
        if (imageDetail.getData() == null) {
            byte[] emptyArray = new byte[0];
            imageDetail.setData(emptyArray);
        }
        int lengthCounter = 0;
        int totalSize = 68 + imageDetail.getData().length;
        byte[] faceArray = new byte[totalSize];
        byte[] recordHeader = this.getFaceGeneralHeader(imageDetail);
        System.arraycopy(recordHeader, 0, faceArray, 0, recordHeader.length);
        lengthCounter = lengthCounter + recordHeader.length;
        byte[] imageHeader = this.getFaceRepresentationHeader(imageDetail);
        System.arraycopy(imageHeader, 0, faceArray, lengthCounter, imageHeader.length);
        lengthCounter += imageHeader.length;
        System.arraycopy(imageDetail.getData(), 0, faceArray, lengthCounter, imageDetail.getData().length);
        int var10000 = lengthCounter + imageDetail.getData().length;
        return faceArray;
    }
    private byte[] getFaceGeneralHeader(ImageDetail imageDetail) {
        byte[] fingerGeneralHeader2011 = new byte[17];
        int lengthCounter = 0;
        String hexFormatIdentifier = "46414300";
        byte[] formatIdentifier = new byte[4];
        formatIdentifier = getByteArray(hexFormatIdentifier, 4);
        System.arraycopy(formatIdentifier, 0, fingerGeneralHeader2011, 0, formatIdentifier.length);
        lengthCounter = lengthCounter + formatIdentifier.length;
        String hexFormatVersion = "30333000";
        byte[] formatVersion = new byte[4];
        formatVersion = getByteArray(hexFormatVersion, 4);
        System.arraycopy(formatVersion, 0, fingerGeneralHeader2011, lengthCounter, formatVersion.length);
        lengthCounter += formatVersion.length;
        int totalSize = 68 + imageDetail.getData().length;
        String lengthStr = Integer.toHexString(totalSize);
        byte[] recordLength = new byte[4];
        recordLength = getByteArray(lengthStr, 4);
        System.arraycopy(recordLength, 0, fingerGeneralHeader2011, lengthCounter, recordLength.length);
        lengthCounter += recordLength.length;
        String No_fingerRep = "01";
        byte[] No_Finger = new byte[2];
        No_Finger = getByteArray(No_fingerRep, 2);
        System.arraycopy(No_Finger, 0, fingerGeneralHeader2011, lengthCounter, No_Finger.length);
        lengthCounter += No_Finger.length;
        String CertificationFlag = "00";
        byte[] CertifFlag = new byte[1];
        CertifFlag = getByteArray(CertificationFlag, 1);
        System.arraycopy(CertifFlag, 0, fingerGeneralHeader2011, lengthCounter, CertifFlag.length);
        lengthCounter += CertifFlag.length;
        String temp_samantics = "0000";
        byte[] temporal_Semantics = new byte[2];
        temporal_Semantics = getByteArray(temp_samantics, 1);
        System.arraycopy(temporal_Semantics, 0, fingerGeneralHeader2011, lengthCounter, temporal_Semantics.length);
        int var10000 = lengthCounter + temporal_Semantics.length;
        return fingerGeneralHeader2011;
    }

    private byte[] getFaceRepresentationHeader(ImageDetail imageDetail) {
        byte[] imageHeaderArray = new byte[51];
        int lengthCounter = 0;
        int RepresentationSize = 51 + imageDetail.getData().length;
        String Representation_Length = Integer.toHexString(RepresentationSize);
        byte[] Rep_Length = new byte[4];
        Rep_Length = getByteArray(Representation_Length, 4);
        System.arraycopy(Rep_Length, 0, imageHeaderArray, lengthCounter, Rep_Length.length);
        lengthCounter = lengthCounter + Rep_Length.length;
        Date date = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yy.MM.dd.hh.mm.ss");
        String str = "Current Date: " + ft.format(date);
        SimpleDateFormat yrs = new SimpleDateFormat("yyyy");
        String year = yrs.format(date);
        int yy = Integer.parseInt(year);
        String yyyy = Integer.toHexString(yy);
        byte[] yr = new byte[2];
        yr = getByteArray(yyyy, 2);
        System.arraycopy(yr, 0, imageHeaderArray, lengthCounter, yr.length);
        lengthCounter += yr.length;
        SimpleDateFormat mnths = new SimpleDateFormat("MM");
        String month = mnths.format(date);
        int MM = Integer.parseInt(month);
        String mmmm = Integer.toHexString(MM);
        byte[] mon = new byte[1];
        mon = getByteArray(mmmm, 1);
        System.arraycopy(mon, 0, imageHeaderArray, lengthCounter, mon.length);
        lengthCounter += mon.length;
        SimpleDateFormat dys = new SimpleDateFormat("dd");
        String day = dys.format(date);
        int dd = Integer.parseInt(day);
        String dddd = Integer.toHexString(dd);
        byte[] dy = new byte[1];
        dy = getByteArray(dddd, 1);
        System.arraycopy(dy, 0, imageHeaderArray, lengthCounter, dy.length);
        lengthCounter += dy.length;
        SimpleDateFormat hrs = new SimpleDateFormat("hh");
        String hour = hrs.format(date);
        int hh = Integer.parseInt(hour);
        String hhhh = Integer.toHexString(hh);
        byte[] hr = new byte[1];
        hr = getByteArray(hhhh, 1);
        System.arraycopy(hr, 0, imageHeaderArray, lengthCounter, hr.length);
        lengthCounter += hr.length;
        SimpleDateFormat mins = new SimpleDateFormat("mm");
        String min = mins.format(date);
        int mm = Integer.parseInt(min);
        String minutes = Integer.toHexString(mm);
        byte[] mn = new byte[1];
        mn = getByteArray(minutes, 1);
        System.arraycopy(mn, 0, imageHeaderArray, lengthCounter, mn.length);
        lengthCounter += mn.length;
        SimpleDateFormat secs = new SimpleDateFormat("ss");
        String sec = secs.format(date);
        int ss = Integer.parseInt(sec);
        String ssss = Integer.toHexString(ss);
        byte[] sc = new byte[1];
        sc = getByteArray(ssss, 1);
        System.arraycopy(sc, 0, imageHeaderArray, lengthCounter, sc.length);
        lengthCounter += sc.length;
        String paddedbytes = "FFFF";
        byte[] p_bytes = new byte[2];
        p_bytes = getByteArray(paddedbytes, 2);
        System.arraycopy(p_bytes, 0, imageHeaderArray, lengthCounter, p_bytes.length);
        lengthCounter += p_bytes.length;
        String DeviceTechnologyIdentifier = "01";
        byte[] TechnologyIdentifier = new byte[1];
        TechnologyIdentifier = getByteArray(DeviceTechnologyIdentifier, 1);
        System.arraycopy(TechnologyIdentifier, 0, imageHeaderArray, lengthCounter, TechnologyIdentifier.length);
        lengthCounter += TechnologyIdentifier.length;
        String DevVendorID = "01FF";
        byte[] VendorID = new byte[2];
        VendorID = getByteArray(DevVendorID, 2);
        System.arraycopy(VendorID, 0, imageHeaderArray, lengthCounter, VendorID.length);
        lengthCounter += VendorID.length;
        String CaptureDevID = "00";
        byte[] DevID = new byte[2];
        DevID = getByteArray(CaptureDevID, 2);
        System.arraycopy(DevID, 0, imageHeaderArray, lengthCounter, DevID.length);
        lengthCounter += DevID.length;
        String QualityBlock = "00";
        byte[] QualBlock = new byte[1];
        QualBlock = getByteArray(QualityBlock, 1);
        System.arraycopy(QualBlock, 0, imageHeaderArray, lengthCounter, QualBlock.length);
        lengthCounter += QualBlock.length;
        String LandmarkPoints = "00";
        byte[] landmarkPoints = new byte[2];
        landmarkPoints = getByteArray(LandmarkPoints, 2);
        System.arraycopy(landmarkPoints, 0, imageHeaderArray, lengthCounter, landmarkPoints.length);
        lengthCounter += landmarkPoints.length;
        String Gender = "00";
        byte[] gender = new byte[1];
        gender = getByteArray(Gender, 1);
        System.arraycopy(gender, 0, imageHeaderArray, lengthCounter, gender.length);
        lengthCounter += gender.length;
        String EyeColor = "00";
        byte[] eyeColor = new byte[1];
        eyeColor = getByteArray(EyeColor, 1);
        System.arraycopy(eyeColor, 0, imageHeaderArray, lengthCounter, eyeColor.length);
        lengthCounter += eyeColor.length;
        String HairColor = "00";
        byte[] hairColor = new byte[1];
        hairColor = getByteArray(HairColor, 1);
        System.arraycopy(hairColor, 0, imageHeaderArray, lengthCounter, hairColor.length);
        lengthCounter += hairColor.length;
        String SubHeight = "00";
        byte[] subHeight = new byte[1];
        subHeight = getByteArray(SubHeight, 1);
        System.arraycopy(subHeight, 0, imageHeaderArray, lengthCounter, subHeight.length);
        lengthCounter += subHeight.length;
        String PropertyMask = "00";
        byte[] propertyMask = new byte[3];
        propertyMask = getByteArray(PropertyMask, 3);
        System.arraycopy(propertyMask, 0, imageHeaderArray, lengthCounter, propertyMask.length);
        lengthCounter += propertyMask.length;
        String ExpressionMask = "00";
        byte[] expressionMask = new byte[2];
        expressionMask = getByteArray(ExpressionMask, 2);
        System.arraycopy(expressionMask, 0, imageHeaderArray, lengthCounter, expressionMask.length);
        lengthCounter += expressionMask.length;
        String PoseAngle = "000";
        byte[] poseAngle = new byte[3];
        poseAngle = getByteArray(PoseAngle, 3);
        System.arraycopy(poseAngle, 0, imageHeaderArray, lengthCounter, poseAngle.length);
        lengthCounter += poseAngle.length;
        String PoseAngleUncertain = "000";
        byte[] poseAngleUncertain = new byte[3];
        poseAngleUncertain = getByteArray(PoseAngleUncertain, 3);
        System.arraycopy(poseAngleUncertain, 0, imageHeaderArray, lengthCounter, poseAngleUncertain.length);
        lengthCounter += poseAngleUncertain.length;
        String ImageType = "00";
        byte[] imgtype = new byte[1];
        imgtype = getByteArray(ImageType, 1);
        System.arraycopy(imgtype, 0, imageHeaderArray, lengthCounter, imgtype.length);
        lengthCounter += imgtype.length;
        if (imageDetail.getFormat() == null) {
//            logger.info("No image format specified, hence setting to jp2");
            imageDetail.setFormat("02");
        }

        String ImageFormat = imageDetail.getFormat();
        byte[] ImgFormat = new byte[1];
        ImgFormat = getByteArray(ImageFormat, 1);
        System.arraycopy(ImgFormat, 0, imageHeaderArray, lengthCounter, ImgFormat.length);
        lengthCounter += ImgFormat.length;
        int imgWidth = imageDetail.getWidth();
        String imagewidth = Integer.toHexString(imgWidth);
        byte[] Image_Width = new byte[2];
        Image_Width = getByteArray(imagewidth, 2);
        System.arraycopy(Image_Width, 0, imageHeaderArray, lengthCounter, Image_Width.length);
        lengthCounter += Image_Width.length;
        int imgHeight = imageDetail.getHeight();
        String imageheight = Integer.toHexString(imgHeight);
        byte[] Image_Height = new byte[2];
        Image_Height = getByteArray(imageheight, 2);
        System.arraycopy(Image_Height, 0, imageHeaderArray, lengthCounter, Image_Height.length);
        lengthCounter += Image_Height.length;
        String SampleRate = "01";
        byte[] sampleRate = new byte[1];
        sampleRate = getByteArray(SampleRate, 1);
        System.arraycopy(sampleRate, 0, imageHeaderArray, lengthCounter, sampleRate.length);
        lengthCounter += sampleRate.length;
        String PostAcq = "0001";
        byte[] postAcq = new byte[2];
        postAcq = getByteArray(PostAcq, 2);
        System.arraycopy(postAcq, 0, imageHeaderArray, lengthCounter, postAcq.length);
        lengthCounter += postAcq.length;
        String CrossRef = "00";
        byte[] crossRef = new byte[1];
        postAcq = getByteArray(CrossRef, 1);
        System.arraycopy(crossRef, 0, imageHeaderArray, lengthCounter, crossRef.length);
        lengthCounter += crossRef.length;
        String ColorSpace = "00";
        byte[] colorSpace = new byte[1];
        colorSpace = getByteArray(ColorSpace, 1);
        System.arraycopy(colorSpace, 0, imageHeaderArray, lengthCounter, colorSpace.length);
        lengthCounter += colorSpace.length;
        int ImageSize = imageDetail.getData().length;
        String ImageLength = Integer.toHexString(ImageSize);
        byte[] ImgLength = new byte[4];
        ImgLength = getByteArray(ImageLength, 4);
        System.arraycopy(ImgLength, 0, imageHeaderArray, lengthCounter, ImgLength.length);
        int var10000 = lengthCounter + ImgLength.length;
        return imageHeaderArray;
    }

    public static byte[] getByteArray(String value, int byteArraylength) {
        byte[] requiredArray = new byte[byteArraylength];
        byte[] originalArray = (new BigInteger(value, 16)).toByteArray();
        int orgLength = originalArray.length;
        int destPos = byteArraylength - orgLength;
        if (orgLength < byteArraylength) {
            System.arraycopy(originalArray, 0, requiredArray, destPos, orgLength);
        } else if (destPos == 0) {
            requiredArray = originalArray;
        }

        return requiredArray;
    }
}
