package com.sandc.enterprise.classifier;

import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by davey.belliss on 7/19/2017.
 */
public class CaseDetails {

  private String       subject;
  private String       type;
  private String       productGroup;
  private String       problemSubType;
  private String       productGroupSubType;
  private String       description;
  private String       timeStamp;
  private List<String> tags;
  private String caseNum;
  private String caseID;

  public String getCaseNum() {
    return caseNum;
  }

  public void setCaseNum(String caseNum) {
    this.caseNum = caseNum;
  }

  public String getCaseID() {
    return caseID;
  }

  public void setCaseID(String caseID) {
    this.caseID = caseID;
  }




  public CaseDetails(String subject, String description) {
    if (StringUtils.isBlank(subject)) {
      throw new IllegalArgumentException("CaseDetails subject cannot be blank");
    }
    if (StringUtils.isBlank(description)) {
      throw new IllegalArgumentException("CaseDetails description cannot be blank");
    }
    if (subject.length() > 254) {
      this.subject = subject.substring(0,250) + "...";
    }// If greater than max length Salesforce can take, truncate
    else {
      this.subject = subject;
    }
    if (description.length() > 32000) {
      this.description = description.substring(0,31980);
      this.description += "\n Truncated \n";
    }// If greater than max length Salesforce can take, truncate
    else {
      this.description = description;
    }

    timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
  }

  @Override
  public String toString() {
    return "CaseDetails{" + "subject='" + subject + '\'' + ", type='" + type + '\'' + ", productGroup='" + productGroup
        + '\'' + ", problemSubType='" + problemSubType + '\'' + ", productGroupSubType='" + productGroupSubType + '\''
        + ", description='" + description + '\'' + ", timeStamp='" + timeStamp + '\'' + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    CaseDetails that = (CaseDetails) o;

    if (!subject.equals(that.subject))
      return false;
    if (type != null ? !type.equals(that.type) : that.type != null)
      return false;
    if (productGroup != null ? !productGroup.equals(that.productGroup) : that.productGroup != null)
      return false;
    if (problemSubType != null ? !problemSubType.equals(that.problemSubType) : that.problemSubType != null)
      return false;
    if (productGroupSubType != null ?
        !productGroupSubType.equals(that.productGroupSubType) :
        that.productGroupSubType != null)
      return false;
    if (!description.equals(that.description))
      return false;
    return timeStamp.equals(that.timeStamp);
  }

  @Override
  public int hashCode() {
    int result = subject.hashCode();
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (productGroup != null ? productGroup.hashCode() : 0);
    result = 31 * result + (problemSubType != null ? problemSubType.hashCode() : 0);
    result = 31 * result + (productGroupSubType != null ? productGroupSubType.hashCode() : 0);
    result = 31 * result + description.hashCode();
    result = 31 * result + timeStamp.hashCode();
    return result;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setProductGroup(String productGroup) {
    this.productGroup = productGroup;
  }

  public void setProblemSubType(String problemSubType) {
    this.problemSubType = problemSubType;
  }

  public void setProductGroupSubType(String productGroupSubType) {
    this.productGroupSubType = productGroupSubType;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getTags() {
    return tags;
  }

  public String getSubject() {
    return subject;
  }

  public String getType() {
    return type;
  }

  public String getProductGroup() {
    return productGroup;
  }

  public String getProblemSubType() {
    return problemSubType;
  }

  public String getProductGroupSubType() {
    return productGroupSubType;
  }

  public String getDescription() {
    return description;
  }

  public String getTimeStamp() {
    return timeStamp;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }




}
