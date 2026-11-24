# Validation Steps

## Overview

Validation steps allow you to verify that your automation tasks completed successfully. They help ensure reliability and catch errors before they propagate through your workflow.

## What Are Validation Steps?

Validation steps are optional instructions that check whether the automation achieved the desired outcome. They run after the main automation steps and verify the results.

## When to Use Validation Steps

✅ **Use validation steps when**:
- Verifying critical operations (login, payment, data submission)
- Ensuring data was extracted correctly
- Confirming navigation succeeded
- Checking for error messages
- Validating form submissions

❌ **Skip validation steps when**:
- Performing simple, low-risk operations
- Speed is more important than verification
- The operation provides clear success/failure feedback

## How Validation Works

1. **Main Steps Execute**: Your automation prompt runs first
2. **Validation Runs**: Validation steps execute on the resulting page state
3. **Results Checked**: Validation results are included in the response
4. **Success/Failure**: Response indicates if validation passed or failed

## Writing Effective Validation Steps

### Basic Validation Patterns

#### Verify Element Presence

```
Validation: "Verify that the success message is displayed"
Validation: "Check that the user menu is visible in the top right"
Validation: "Confirm the product was added to cart"
```

#### Verify Text Content

```
Validation: "Verify the page title contains 'Dashboard'"
Validation: "Check that the confirmation message says 'Order placed successfully'"
Validation: "Confirm the error message is not displayed"
```

#### Verify Navigation

```
Validation: "Verify we are on the dashboard page (URL contains '/dashboard')"
Validation: "Check that the current URL is https://example.com/success"
Validation: "Confirm we navigated away from the login page"
```

#### Verify Data

```
Validation: "Verify that at least 10 products are displayed"
Validation: "Check that all prices are greater than $0"
Validation: "Confirm the extracted data contains a 'name' field"
```

## Examples by Use Case

### Example 1: Login Validation

**Main Steps**:
```
Navigate to https://example.com/login
Enter 'user@example.com' in the email field
Enter 'password123' in the password field
Click the 'Login' button
```

**Validation Steps**:
```
Verify that the dashboard page is displayed
Check that the user menu shows the email 'user@example.com'
Confirm no error messages are visible
```

### Example 2: Form Submission Validation

**Main Steps**:
```
Navigate to https://example.com/contact
Fill in name: 'John Doe'
Fill in email: 'john@example.com'
Fill in message: 'Test message'
Click 'Submit'
```

**Validation Steps**:
```
Verify that a success message appears
Check that the message says 'Thank you for contacting us'
Confirm the form fields are cleared or the page redirected
```

### Example 3: Data Extraction Validation

**Main Steps**:
```
Navigate to https://example.com/products
Extract product names and prices from all items
```

**Validation Steps**:
```
Verify that at least 5 products were extracted
Check that each product has both a name and price
Confirm all prices are in the format $XX.XX
Verify no product names are empty
```

### Example 4: File Download Validation

**Main Steps**:
```
Navigate to https://example.com/reports
Click the 'Download Report' button
Wait for download to complete
```

**Validation Steps**:
```
Verify that a file was downloaded
Check that the filename contains 'report'
Confirm the file size is greater than 0 bytes
```

## Validation Response

When validation steps are included, the response contains validation results:

```json
{
  "status": "success",
  "data": {
    "productName": "Example Product",
    "price": "$29.99"
  },
  "validation": {
    "passed": true,
    "checks": [
      {
        "description": "Verify product name is not empty",
        "result": "passed"
      },
      {
        "description": "Check price is in correct format",
        "result": "passed"
      }
    ]
  },
  "message": "Steps executed successfully, all validations passed"
}
```

## Handling Validation Failures

If validation fails, the response indicates the failure:

```json
{
  "status": "partial_success",
  "data": {
    "productName": "",
    "price": "$29.99"
  },
  "validation": {
    "passed": false,
    "checks": [
      {
        "description": "Verify product name is not empty",
        "result": "failed",
        "error": "Product name is empty"
      },
      {
        "description": "Check price is in correct format",
        "result": "passed"
      }
    ]
  },
  "message": "Steps executed but validation failed"
}
```

## Best Practices

### 1. Be Specific

❌ **Vague**: "Check if it worked"
✅ **Specific**: "Verify that the success message 'Order placed successfully' is displayed"

### 2. Validate Critical Operations

Always validate:
- Authentication (login/logout)
- Data submission (forms, payments)
- Data extraction (ensure data quality)
- File operations (downloads, uploads)

### 3. Use Multiple Checks

```
Validation Steps:
1. Verify the page title is 'Dashboard'
2. Check that the user menu is visible
3. Confirm no error messages are displayed
4. Verify the welcome message contains the user's name
```

### 4. Validate Data Quality

```
Validation Steps:
- Verify all extracted products have names
- Check all prices are numeric and greater than 0
- Confirm all dates are in YYYY-MM-DD format
- Verify no fields contain 'null' or 'undefined'
```

### 5. Check for Errors

```
Validation Steps:
- Verify no error messages are displayed
- Check that the page doesn't show a 404 or 500 error
- Confirm no warning banners are visible
```

## Validation in Different Modes

### Single Command Mode

Include validation in the same request:

```
Prompt: "Navigate to https://example.com and extract the page title"
Validation: "Verify the page title is not empty and contains 'Example'"
```

### Interactive Session Mode

Validate after each critical step:

```
Step 1:
  Prompt: "Navigate to login page"
  Validation: "Verify login form is visible"

Step 2:
  Prompt: "Enter credentials and submit"
  Validation: "Verify we're on the dashboard page"

Step 3:
  Prompt: "Extract user data"
  Validation: "Verify extracted data has all required fields"
```

## Common Validation Patterns

### Page Load Validation

```
"Verify the page has finished loading"
"Check that the loading spinner is not visible"
"Confirm the main content is displayed"
```

### Element State Validation

```
"Verify the submit button is enabled"
"Check that the form fields are not disabled"
"Confirm the checkbox is checked"
```

### Content Validation

```
"Verify the heading text is 'Welcome'"
"Check that the paragraph contains the word 'success'"
"Confirm the list has at least 5 items"
```

### URL Validation

```
"Verify the current URL is https://example.com/success"
"Check that the URL contains '/dashboard'"
"Confirm we're not on the error page"
```

## Next Steps

- Learn about [Writing Effective Prompts](/pages/WritingPrompts.md)
- Review [Error Handling](/pages/ErrorHandling.md) strategies
- Practice with [Single Command Mode](/pages/SingleCommandMode.md)

---

> **💡 Tip**: Always validate critical operations. The small overhead of validation is worth the confidence that your automation succeeded.

